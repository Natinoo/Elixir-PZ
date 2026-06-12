// SORBNET — panel pracownika banku.

const WS_ENDPOINT = "/ws"; 
const BLOCK_AFTER_MS = 2 * 60 * 60 * 1000; // 2 h od przekroczenia limitu do automatycznej blokady

const $ = (id) => document.getElementById(id);
const fmtPLN = new Intl.NumberFormat("pl-PL", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

let currentBank = null;
let currentStatus = null;
let stompClient = null;
let alertSub = null;
let countdownTimer = null;
let banksCache = []; // [{bankId, bankName, accountNumber}]

/* ── start ─────────────────────────────────────────────── */

document.addEventListener("DOMContentLoaded", async () => {
  initDateFilter();
  await loadBanks();
  connectWs();

  $("bankSelect").addEventListener("change", () => selectBank($("bankSelect").value));
  $("fromDate").addEventListener("change", loadPayments);
  $("depositForm").addEventListener("submit", onDeposit);
  $("transferForm").addEventListener("submit", onTransfer);
  $("fillToLimit").addEventListener("click", fillToLimit);
  $("alertClose").addEventListener("click", hideAlert);
  $("alertTopUp").addEventListener("click", () => { hideAlert(); fillToLimit(); $("depositAmount").focus(); });

  setInterval(refreshAll, 10000); // polling awaryjny, gdyby WS nie działał
});

/* ── banki ─────────────────────────────────────────────── */

async function loadBanks() {
  const accounts = await getJson("/api/sorbnet/accounts");
  banksCache = accounts.filter(a => !a.serviceCode || a.serviceCode === "SORBNET");

  const bankSel = $("bankSelect");
  const srcSel = $("depositSource");
  bankSel.innerHTML = "";
  srcSel.innerHTML = "";

  for (const a of banksCache) {
    if (a.bankId !== "NBP") {
      bankSel.appendChild(new Option(`${a.bankId} — ${a.bankName}`, a.bankId));
    }
    srcSel.appendChild(new Option(a.bankId === "NBP" ? "NBP (bank centralny)" : a.bankId, a.bankId));
  }
  srcSel.value = "NBP";

  selectBank(bankSel.value);
}

function selectBank(bankId) {
  currentBank = bankId;
  fillReceiverSelect();
  subscribeBankAlerts(bankId);
  refreshAll();
}

function fillReceiverSelect() {
  const sel = $("transferReceiver");
  sel.innerHTML = "";
  banksCache
    .filter(b => b.bankId !== currentBank)
    .forEach(b => sel.appendChild(new Option(`${b.bankId} — ${b.bankName}`, b.bankId)));
}

async function refreshAll() {
  if (!currentBank) return;
  await Promise.allSettled([refreshStatus(), loadPayments(), loadLiquidity()]);
}

/* ── status rachunku + pasek płynności ─────────────────── */

async function refreshStatus() {
  currentStatus = await getJson(`/api/sorbnet/accounts/${currentBank}/status`);
  const s = currentStatus;

  setMoney("balance", s.balance);
  setMoney("debtLimit", s.debtLimit, false);
  setMoney("availableCredit", s.availableCredit);
  setMoney("minDeposit", s.minDepositToRestore, false);

  const pill = $("bankStatus");
  pill.className = "status-pill";
  if (s.blocked) {
    pill.classList.add("blocked"); pill.textContent = "ZABLOKOWANY";
  } else if (s.overlimitSince) {
    pill.classList.add("danger"); pill.textContent = "PONAD LIMITEM";
  } else if (Number(s.availableCredit) < Number(s.debtLimit) * 0.2) {
    pill.classList.add("warn"); pill.textContent = "NISKA PŁYNNOŚĆ";
  } else {
    pill.classList.add("ok"); pill.textContent = "AKTYWNY";
  }

  updateStrip(s);
}

function updateStrip(s) {
  const strip = $("liquidityStrip");
  const detail = $("stripDetail");
  const cd = $("stripCountdown");
  clearInterval(countdownTimer);
  cd.hidden = true;
  strip.className = "liquidity-strip";

  if (s.blocked) {
    strip.classList.add("blocked");
    $("stripStatus").textContent = "BANK ZABLOKOWANY";
    detail.textContent = "Rozrachunek wstrzymany. Zasil rachunek i poproś operatora o odblokowanie.";
    return;
  }

  if (s.overlimitSince) {
    strip.classList.add("danger");
    $("stripStatus").textContent = "PRZEKROCZONY LIMIT ZADŁUŻENIA";
    detail.textContent = `wymagana wpłata min. ${fmtPLN.format(s.minDepositToRestore)} PLN`;
    cd.hidden = false;
    const deadline = new Date(s.overlimitSince).getTime() + BLOCK_AFTER_MS;
    const tick = () => {
      const left = deadline - Date.now();
      cd.textContent = left > 0
        ? `blokada za ${fmtClock(left)}`
        : "blokada automatyczna w toku";
      if (left <= 0) clearInterval(countdownTimer);
    };
    tick();
    countdownTimer = setInterval(tick, 1000);
    return;
  }

  if (Number(s.availableCredit) < Number(s.debtLimit) * 0.2) {
    strip.classList.add("warn");
    $("stripStatus").textContent = "PŁYNNOŚĆ NA WYCZERPANIU";
    detail.textContent = `dostępne ${fmtPLN.format(s.availableCredit)} PLN`;
    return;
  }

  strip.classList.add("ok");
  $("stripStatus").textContent = "PŁYNNOŚĆ W NORMIE";
  detail.textContent = `dostępne ${fmtPLN.format(s.availableCredit)} PLN`;
}

/* ── nowy przelew SORBNET (ISO 20022 pacs.008) ─────────── */

async function onTransfer(e) {
  e.preventDefault();
  const receiverId = $("transferReceiver").value;
  const amount = Number($("transferAmount").value);
  const title = $("transferTitle").value.trim() || "Rozrachunek międzybankowy";
  const senderName = $("transferSenderName").value.trim();
  const receiverName = $("transferReceiverName").value.trim();

  const sender = banksCache.find(b => b.bankId === currentBank);
  const receiver = banksCache.find(b => b.bankId === receiverId);
  if (!sender || !receiver) { toast("Nie można ustalić rachunków stron przelewu.", true); return; }

  const paymentId = "SORB-GUI-" + Date.now();
  const xml = buildPacs008({
    paymentId, amount, currency: "PLN",
    senderBankId: sender.bankId, receiverBankId: receiver.bankId,
    senderAccount: sender.accountNumber || "", receiverAccount: receiver.accountNumber || "",
    senderName, receiverName,
    title
  });

  try {
    const r = await fetch("/api/sorbnet/payments", {
      method: "POST",
      headers: { "Content-Type": "application/xml", Accept: "application/xml" },
      body: xml
    });
    const text = await r.text();
    if (!r.ok) { toast(extractXml(text, "message") || `Błąd ${r.status}`, true); return; }

    const status = extractXml(text, "TxSts") || "UNKNOWN";
    const info = extractXml(text, "AddtlInf") || "";
    const ok = status === "SETTLED";
    toast(`Przelew ${paymentId}: ${status}${info ? " — " + info : ""}`, !ok && status !== "GRIDLOCK_HELD");

    $("transferAmount").value = "";
    $("transferTitle").value = "";
    $("transferSenderName").value = "";
    $("transferReceiverName").value = "";
    refreshAll();
  } catch (err) {
    toast("Nie udało się wysłać przelewu: " + err.message, true);
  }
}

function buildPacs008(p) {
  const now = new Date().toISOString().slice(0, 19);
  return `<?xml version="1.0" encoding="UTF-8"?>
<Document>
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>${x(p.paymentId)}</MsgId>
      <CreDtTm>${now}</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <TtlIntrBkSttlmAmt Ccy="${x(p.currency)}">${p.amount.toFixed(2)}</TtlIntrBkSttlmAmt>
      <SttlmInf><SttlmMtd>CLRG</SttlmMtd><ClrSys><Cd>SORBNET</Cd></ClrSys></SttlmInf>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <InstrId>${x(p.paymentId)}</InstrId>
        <EndToEndId>${x(p.paymentId)}</EndToEndId>
        <TxId>${x(p.paymentId)}</TxId>
      </PmtId>
      <IntrBkSttlmAmt Ccy="${x(p.currency)}">${p.amount.toFixed(2)}</IntrBkSttlmAmt>
      ${p.senderName ? `<Dbtr><Nm>${x(p.senderName)}</Nm></Dbtr>` : ""}
      <DbtrAcct><Id><IBAN>${x(p.senderAccount)}</IBAN></Id></DbtrAcct>
      <DbtrAgt><FinInstnId><BICFI>${x(p.senderBankId)}</BICFI></FinInstnId></DbtrAgt>
      ${p.receiverName ? `<Cdtr><Nm>${x(p.receiverName)}</Nm></Cdtr>` : ""}
      <CdtrAcct><Id><IBAN>${x(p.receiverAccount)}</IBAN></Id></CdtrAcct>
      <CdtrAgt><FinInstnId><BICFI>${x(p.receiverBankId)}</BICFI></FinInstnId></CdtrAgt>
      <RmtInf><Ustrd>${x(p.title)}</Ustrd></RmtInf>
      <SplmtryData><Envlp>
        <ServiceCode>SORBNET</ServiceCode>
        <SenderBankId>${x(p.senderBankId)}</SenderBankId>
        <ReceiverBankId>${x(p.receiverBankId)}</ReceiverBankId>
      </Envlp></SplmtryData>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>`;
}

function extractXml(xmlText, tag) {
  try {
    const doc = new DOMParser().parseFromString(xmlText, "application/xml");
    const el = doc.getElementsByTagName(tag)[0];
    return el ? el.textContent : null;
  } catch { return null; }
}

function x(v) {
  return String(v ?? "").replace(/[<>&"']/g, c =>
    ({ "<": "&lt;", ">": "&gt;", "&": "&amp;", '"': "&quot;", "'": "&apos;" }[c]));
}

/* ── zasilenie rachunku (NBP jako nieskończone źródło) ─── */

function fillToLimit() {
  const min = Number(currentStatus?.minDepositToRestore ?? 0);
  $("depositAmount").value = min > 0 ? min.toFixed(2) : "";
  if (min <= 0) toast("Bank mieści się w limicie — wpłata nie jest wymagana.");
}

async function onDeposit(e) {
  e.preventDefault();
  const amount = $("depositAmount").value;
  const source = $("depositSource").value;
  try {
    const res = await postJson(
      `/api/sorbnet/accounts/${currentBank}/deposit?amount=${encodeURIComponent(amount)}&sourceBankId=${encodeURIComponent(source)}`);
    toast(`Zasilono rachunek kwotą ${fmtPLN.format(res.depositedAmount)} PLN. Saldo: ${fmtPLN.format(res.balanceAfter)} PLN.`);
    $("depositAmount").value = "";
    refreshAll();
  } catch (err) {
    toast(err.message, true);
  }
}

/* ── wnioski o płynność z ELIXIR-ów ────────────────────── */

async function loadLiquidity() {
  const list = await getJson("/api/sorbnet/liquidity/requests");
  const box = $("liquidityList");
  box.innerHTML = "";

  if (!list.length) {
    box.innerHTML = '<p class="empty">Brak oczekujących wniosków.</p>';
    return;
  }

  for (const r of list) {
    const mine = r.bankId === currentBank;
    const el = document.createElement("div");
    el.className = "liq-item" + (mine ? " mine" : "");
    el.innerHTML = `
      <div class="liq-head">
        <span><span class="liq-bank">${esc(r.bankId)}</span>
              <span class="liq-service">· ${esc(r.requestingServiceCode)}</span></span>
        <span class="liq-amount">${fmtPLN.format(r.amount)} ${esc(r.currency)}</span>
      </div>
      <p class="liq-msg">${esc(r.message || "Brak płynności w sesji")} · sesja ${esc(r.sessionId || "—")}</p>
      <div class="liq-actions"></div>`;

    const actions = el.querySelector(".liq-actions");
    if (mine) {
      actions.append(
        button("Wykonaj przelew", "btn settle", () => actOnRequest(r.requestId, "execute")),
        button("Odrzuć", "btn refuse", () => {
          const reason = prompt("Powód odrzucenia (opcjonalnie):") ?? "";
          actOnRequest(r.requestId, `reject${reason ? "?reason=" + encodeURIComponent(reason) : ""}`);
        })
      );
    } else {
      actions.innerHTML = '<span class="hint">Wniosek innego banku — decyzję podejmuje jego pracownik.</span>';
    }
    box.appendChild(el);
  }
}

async function actOnRequest(requestId, action) {
  try {
    const res = await postJson(`/api/sorbnet/liquidity/requests/${encodeURIComponent(requestId)}/${action}`);
    toast(res.status === "EXECUTED"
      ? `Przelew płynnościowy wykonany (${res.paymentId}).`
      : `Wniosek odrzucony.`);
    refreshAll();
  } catch (err) {
    toast(err.message, true);
  }
}

/* ── przelewy ──────────────────────────────────────────── */

function initDateFilter() {
  const input = $("fromDate");
  const today = new Date();
  const monthAgo = new Date(today); monthAgo.setMonth(monthAgo.getMonth() - 1);
  input.max = isoDate(today);
  input.min = isoDate(monthAgo);
  input.value = isoDate(today);
}

async function loadPayments() {
  if (!currentBank) return;
  const payments = await getJson(`/api/sorbnet/operator/payments?bankId=${encodeURIComponent(currentBank)}`);
  const from = new Date($("fromDate").value + "T00:00:00");

  const rows = payments
    .filter(p => p.createdAt && new Date(p.createdAt) >= from)
    .sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));

  const body = $("paymentsBody");
  body.innerHTML = rows.length ? "" : '<tr><td colspan="8" class="empty">Brak przelewów w wybranym okresie.</td></tr>';

  for (const p of rows) body.appendChild(paymentRow(p));
}

function paymentRow(p, fresh = false) {
  const out = p.senderBankId === currentBank;
  const tr = document.createElement("tr");
  if (fresh) tr.className = "fresh";
  tr.innerHTML = `
    <td class="pid">${esc(p.paymentId)}</td>
    <td><span class="dir ${out ? "out" : "in"}">${out ? "WYCH →" : "← PRZYCH"}</span></td>
    <td>${esc(out ? (p.receiverName ? p.receiverName + " · " : "") + p.receiverBankId : (p.senderName ? p.senderName + " · " : "") + p.senderBankId)}</td>
    <td>${esc(p.title || "—")}</td>
    <td class="liq-service">${esc(p.sourceService || "SORBNET")}</td>
    <td class="num ${out ? "neg" : "pos"}">${out ? "−" : "+"}${fmtPLN.format(p.amount)}</td>
    <td>${esc(p.status)}</td>
    <td class="pid">${p.settledAt ? esc(p.settledAt.replace("T", " ").slice(0, 19)) : "—"}</td>`;
  return tr;
}

/* ── WebSocket / STOMP ─────────────────────────────────── */

function connectWs() {
  try {
    const sock = new SockJS(WS_ENDPOINT);
    stompClient = Stomp.over(sock);
    stompClient.debug = null;

    stompClient.connect({}, () => {
      $("wsDot").classList.add("on");

      stompClient.subscribe("/topic/payments", () => { refreshStatus(); loadPayments(); });
      stompClient.subscribe("/topic/liquidity", (msg) => {
        loadLiquidity();
        const ev = safeJson(msg.body);
        if (ev?.type === "LIQUIDITY_REQUEST" && ev.bankId === currentBank) showLiquidityAlert(ev);
      });
      stompClient.subscribe("/topic/gridlock", () => refreshStatus());

      subscribeBankAlerts(currentBank);
    }, () => {
      $("wsDot").classList.remove("on");
      setTimeout(connectWs, 5000); // reconnect
    });
  } catch (e) {
    console.warn("WS niedostępny — działa tryb odpytywania.", e);
  }
}

function subscribeBankAlerts(bankId) {
  if (!stompClient || !stompClient.connected || !bankId) return;
  if (alertSub) alertSub.unsubscribe();
  alertSub = stompClient.subscribe("/topic/alerts/" + bankId, (msg) => {
    const a = safeJson(msg.body);
    if (a?.alert) showAlert(a);
    refreshStatus();
  });
}

/* ── popup alertów ─────────────────────────────────────── */

function showAlert(a) {
  const titles = {
    DEBT_LIMIT_EXCEEDED: "Przekroczono limit zadłużenia",
    APPROACHING_DEBT_LIMIT: "Saldo zbliża się do limitu zadłużenia",
    LIQUIDITY_REQUEST: "Wniosek o płynność z systemu ELIXIR",
    BANK_BLOCKED: "Bank został zablokowany"
  };
  $("alertTitle").textContent = titles[a.type] || "Alert systemu SORBNET";
  $("alertBody").textContent = a.message || "";

  const dl = $("alertDeadline");
  if (a.type === "DEBT_LIMIT_EXCEEDED" && a.blockedIfNotResolvedBy) {
    dl.hidden = false;
    dl.textContent = "Automatyczna blokada: " + a.blockedIfNotResolvedBy.replace("T", " ").slice(0, 19);
  } else {
    dl.hidden = true;
  }

  $("alertTopUp").hidden = !(a.type === "DEBT_LIMIT_EXCEEDED" || a.type === "APPROACHING_DEBT_LIMIT");
  $("alertOverlay").hidden = false;
}

function showLiquidityAlert(ev) {
  showAlert({
    alert: true,
    type: "LIQUIDITY_REQUEST",
    message: `Serwis ${ev.requestingServiceCode} zgłasza brak ${fmtPLN.format(ev.amount)} ${ev.currency} `
           + `na rozliczenie sesji ${ev.sessionId || ""}. Zdecyduj o przelewie w sekcji „Wnioski o płynność”.`
  });
}

function hideAlert() { $("alertOverlay").hidden = true; }

/* ── narzędzia ─────────────────────────────────────────── */

async function getJson(url) {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) throw new Error(await errText(r));
  return r.json();
}

async function postJson(url) {
  const r = await fetch(url, { method: "POST", headers: { Accept: "application/json" } });
  if (!r.ok) throw new Error(await errText(r));
  return r.json();
}

async function errText(r) {
  try {
    const body = await r.json();
    return body.message || body.error || `Błąd ${r.status}`;
  } catch { return `Błąd ${r.status}`; }
}

function setMoney(id, value, signed = true) {
  const el = $(id);
  const n = Number(value ?? 0);
  el.textContent = fmtPLN.format(n) + " PLN";
  el.classList.remove("neg", "pos");
  if (signed && n < 0) el.classList.add("neg");
}

function button(label, cls, onClick) {
  const b = document.createElement("button");
  b.className = cls; b.textContent = label; b.addEventListener("click", onClick);
  return b;
}

function toast(text, isErr = false) {
  const t = $("toast");
  t.textContent = text;
  t.className = "toast" + (isErr ? " err" : "");
  t.hidden = false;
  clearTimeout(t._timer);
  t._timer = setTimeout(() => { t.hidden = true; }, 5000);
}

function fmtClock(ms) {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = String(Math.floor(s / 3600)).padStart(2, "0");
  const m = String(Math.floor((s % 3600) / 60)).padStart(2, "0");
  const sec = String(s % 60).padStart(2, "0");
  return `${h}:${m}:${sec}`;
}

function isoDate(d) { return d.toISOString().slice(0, 10); }
function safeJson(s) { try { return JSON.parse(s); } catch { return null; } }
function esc(v) {
  return String(v ?? "").replace(/[&<>"']/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
/* SORBNET — panel operatora systemu. */

const WS_ENDPOINT = "/ws"; // TODO: jak w app.js — dopasuj do WebSocketConfig
const $ = (id) => document.getElementById(id);
const fmtPLN = new Intl.NumberFormat("pl-PL", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

document.addEventListener("DOMContentLoaded", () => {
  initTabs();
  refreshAll();
  connectWs();

  $("payRange").addEventListener("change", loadPayments);
  $("payStatus").addEventListener("change", loadPayments);
  $("payBank").addEventListener("change", loadPayments);

  setInterval(refreshAll, 12000);
});

function initTabs() {
  document.querySelectorAll(".tab").forEach(btn => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach(b => b.classList.remove("active"));
      document.querySelectorAll(".tab-pane").forEach(p => p.classList.remove("active"));
      btn.classList.add("active");
      $("pane-" + btn.dataset.tab).classList.add("active");
    });
  });
}

async function refreshAll() {
  await Promise.allSettled([
    loadAccounts(), loadPayments(), loadNetting(), loadGridlock(), loadEmergencies()
  ]);
}

/* ── Rachunki + blokady ───────────────────────────────── */

async function loadAccounts() {
  const banks = (await getJson("/api/sorbnet/operator/banks"))
    .filter(b => !b.serviceCode || b.serviceCode === "SORBNET");

  // selektor banku w zakładce transakcji
  const sel = $("payBank");
  const chosen = sel.value;
  sel.innerHTML = '<option value="">wszystkie</option>';
  banks.forEach(b => sel.appendChild(new Option(b.bankId, b.bankId)));
  sel.value = chosen;

  const body = $("accountsBody");
  body.innerHTML = "";
  for (const b of banks) {
    const available = Number(b.balance) + Number(b.debtLimit);
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(b.bankId)}</td>
      <td>${esc(b.bankName)}</td>
      <td class="num ${Number(b.balance) < 0 ? "neg" : ""}">${fmtPLN.format(b.balance)}</td>
      <td class="num">${fmtPLN.format(b.debtLimit)}</td>
      <td class="num ${available < 0 ? "neg" : "pos"}">${fmtPLN.format(available)}</td>
      <td class="pid">${b.overlimitSince ? esc(fmtTs(b.overlimitSince)) : "—"}</td>
      <td>${statusPill(b)}</td>
      <td></td>`;
    tr.lastElementChild.appendChild(
      b.blocked
        ? button("Odblokuj", "btn settle", () => toggleBlock(b.bankId, "unblock"))
        : button("Zablokuj", "btn refuse", () => toggleBlock(b.bankId, "block"))
    );
    body.appendChild(tr);
  }
}

async function toggleBlock(bankId, action) {
  if (action === "block" && !confirm(`Zablokować udział banku ${bankId} w systemie?`)) return;
  try {
    const res = await postJson(`/api/sorbnet/operator/banks/${encodeURIComponent(bankId)}/${action}`);
    toast(`Bank ${res.bankId}: ${res.status === "BLOCKED" ? "zablokowany" : "odblokowany"}.`);
    refreshAll();
  } catch (e) { toast(e.message, true); }
}

function statusPill(b) {
  if (b.blocked) return '<span class="status-pill blocked">ZABLOKOWANY</span>';
  if (b.overlimitSince) return '<span class="status-pill danger">PONAD LIMITEM</span>';
  return '<span class="status-pill ok">AKTYWNY</span>';
}

/* ── Transakcje ───────────────────────────────────────── */

async function loadPayments() {
  const params = new URLSearchParams();
  if ($("payStatus").value) params.set("status", $("payStatus").value);
  if ($("payBank").value) params.set("bankId", $("payBank").value);

  let payments = await getJson("/api/sorbnet/operator/payments" + (params.size ? "?" + params : ""));

  if ($("payRange").value === "today") {
    const start = new Date(); start.setHours(0, 0, 0, 0);
    payments = payments.filter(p => p.createdAt && new Date(p.createdAt) >= start);
  }
  payments.sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));

  const body = $("paymentsBody");
  body.innerHTML = payments.length ? "" : '<tr><td colspan="9" class="empty">Brak transakcji dla wybranych filtrów.</td></tr>';
  for (const p of payments) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(p.paymentId)}</td>
      <td>${esc(p.senderBankId)}</td>
      <td>${esc(p.receiverBankId)}</td>
      <td>${esc(p.title || "—")}</td>
      <td class="liq-service">${esc(p.sourceService || "SORBNET")}</td>
      <td class="num">${fmtPLN.format(p.amount)}</td>
      <td>${esc(p.status)}</td>
      <td class="pid">${p.createdAt ? esc(fmtTs(p.createdAt)) : "—"}</td>
      <td class="pid">${p.settledAt ? esc(fmtTs(p.settledAt)) : "—"}</td>`;
    body.appendChild(tr);
  }
}

/* ── Netting i płynność ───────────────────────────────── */

async function loadNetting() {
  const [payments, liq] = await Promise.all([
    getJson("/api/sorbnet/operator/payments"),
    getAllLiquidity()
  ]);

  const fromElixirs = payments
    .filter(p => p.sourceService === "ELIXIR" || p.sourceService === "ELIXIR_EXPRESS")
    .sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));

  const nb = $("nettingBody");
  nb.innerHTML = fromElixirs.length ? "" : '<tr><td colspan="8" class="empty">Brak rozrachunków z systemów ELIXIR.</td></tr>';
  for (const p of fromElixirs) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(p.paymentId)}</td>
      <td class="liq-service">${esc(p.sourceService)}</td>
      <td>${esc(p.senderBankId)}</td>
      <td>${esc(p.receiverBankId)}</td>
      <td>${esc(p.title || "—")}</td>
      <td class="num">${fmtPLN.format(p.amount)}</td>
      <td>${esc(p.status)}</td>
      <td class="pid">${p.settledAt ? esc(fmtTs(p.settledAt)) : "—"}</td>`;
    nb.appendChild(tr);
  }

  const lb = $("liqHistoryBody");
  lb.innerHTML = liq.length ? "" : '<tr><td colspan="8" class="empty">Brak wniosków o płynność.</td></tr>';
  for (const r of liq) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(r.requestId)}</td>
      <td class="pid">${esc(r.sessionId || "—")}</td>
      <td>${esc(r.bankId)}</td>
      <td class="liq-service">${esc(r.requestingServiceCode)}</td>
      <td class="num">${fmtPLN.format(r.amount)} ${esc(r.currency)}</td>
      <td>${esc(r.status)}</td>
      <td class="pid">${esc(fmtTs(r.receivedAt))}</td>
      <td class="pid">${r.processedAt ? esc(fmtTs(r.processedAt)) : "—"}</td>`;
    lb.appendChild(tr);
  }
}

async function getAllLiquidity() {
    const res = await fetch("/liquidity/requests/all");
    return res.json();
}

/* ── Gridlock + sesje do upłynnienia ──────────────────── */

async function loadGridlock() {
  const [held, pending] = await Promise.all([
    getJson("/api/sorbnet/operator/gridlock"),
    getJson("/api/sorbnet/liquidity/requests")
  ]);

  setBadge("gridlockBadge", held.length + pending.length);

  const gb = $("gridlockBody");
  gb.innerHTML = held.length ? "" : '<tr><td colspan="6" class="empty">Kolejka gridlock jest pusta.</td></tr>';
  for (const p of held) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(p.paymentId)}</td>
      <td>${esc(p.senderBankId)}</td>
      <td>${esc(p.receiverBankId)}</td>
      <td>${esc(p.title || "—")}</td>
      <td class="num">${fmtPLN.format(p.amount)}</td>
      <td class="pid">${p.createdAt ? esc(fmtTs(p.createdAt)) : "—"}</td>`;
    gb.appendChild(tr);
  }

  const pb = $("liqPendingBody");
  pb.innerHTML = pending.length ? "" : '<tr><td colspan="6" class="empty">Żadna sesja nie czeka na upłynnienie.</td></tr>';
  for (const r of pending) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(r.requestId)}</td>
      <td class="pid">${esc(r.sessionId || "—")}</td>
      <td>${esc(r.bankId)}</td>
      <td class="liq-service">${esc(r.requestingServiceCode)}</td>
      <td class="num neg">${fmtPLN.format(r.amount)} ${esc(r.currency)}</td>
      <td class="pid">${esc(fmtTs(r.receivedAt))}</td>`;
    pb.appendChild(tr);
  }
}

/* ── Emergency ────────────────────────────────────────── */

async function loadEmergencies() {
  const banks = await getJson("/api/sorbnet/operator/emergencies");
  setBadge("emergencyBadge", banks.length);

  const body = $("emergencyBody");
  body.innerHTML = banks.length ? "" : '<tr><td colspan="7" class="empty">Brak banków w stanie alarmowym.</td></tr>';
  for (const b of banks) {
    const missing = Math.max(0, -(Number(b.balance) + Number(b.debtLimit)));
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td class="pid">${esc(b.bankId)}</td>
      <td class="num neg">${fmtPLN.format(b.balance)}</td>
      <td class="num">${fmtPLN.format(b.debtLimit)}</td>
      <td class="num neg">${fmtPLN.format(missing)}</td>
      <td class="pid">${b.overlimitSince ? esc(fmtTs(b.overlimitSince)) : "—"}</td>
      <td>${statusPill(b)}</td>
      <td></td>`;
    tr.lastElementChild.appendChild(
      b.blocked
        ? button("Odblokuj", "btn settle", () => toggleBlock(b.bankId, "unblock"))
        : button("Zablokuj", "btn refuse", () => toggleBlock(b.bankId, "block"))
    );
    body.appendChild(tr);
  }
}

/* ── WebSocket / STOMP ────────────────────────────────── */

function connectWs() {
  try {
    const sock = new SockJS(WS_ENDPOINT);
    const stomp = Stomp.over(sock);
    stomp.debug = null;

    stomp.connect({}, () => {
      $("wsDot").classList.add("on");
      stomp.subscribe("/topic/payments", () => { loadPayments(); loadNetting(); });
      stomp.subscribe("/topic/gridlock", (m) => { logEvent("GRIDLOCK", m.body); loadGridlock(); loadEmergencies(); });
      stomp.subscribe("/topic/emergency", (m) => { logEvent("EMERGENCY", m.body); loadEmergencies(); loadAccounts(); });
      stomp.subscribe("/topic/liquidity", (m) => { logEvent("PŁYNNOŚĆ", m.body); loadGridlock(); loadNetting(); });
      stomp.subscribe("/topic/settlements", (m) => logEvent("ROZRACHUNEK", m.body));
    }, () => {
      $("wsDot").classList.remove("on");
      setTimeout(connectWs, 5000);
    });
  } catch (e) {
    console.warn("WS niedostępny — działa tryb odpytywania.", e);
  }
}

function logEvent(kind, raw) {
  const log = $("eventLog");
  if (log.querySelector(".empty")) log.innerHTML = "";

  const ev = safeJson(raw) || {};
  const line = document.createElement("div");
  line.className = "event-line";
  line.innerHTML = `
    <span class="pid">${new Date().toLocaleTimeString("pl-PL")}</span>
    <span class="event-kind">${esc(kind)}</span>
    <span>${esc(ev.type || "")} ${esc(ev.bankId || "")} ${ev.amount != null ? fmtPLN.format(ev.amount) + " PLN" : ""} ${esc(ev.message || "")}</span>`;
  log.prepend(line);
  while (log.children.length > 60) log.lastChild.remove();
}

/* ── narzędzia ────────────────────────────────────────── */

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
  try { const b = await r.json(); return b.message || b.error || `Błąd ${r.status}`; }
  catch { return `Błąd ${r.status}`; }
}

function setBadge(id, n) {
  const el = $(id);
  el.hidden = n === 0;
  el.textContent = n;
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

function fmtTs(ts) { return String(ts).replace("T", " ").slice(0, 19); }
function safeJson(s) { try { return JSON.parse(s); } catch { return null; } }
function esc(v) {
  return String(v ?? "").replace(/[&<>"']/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
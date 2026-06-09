import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const BASE = 'http://localhost:8083';

async function apiFetchText(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
  });

  const raw = await res.text();
  console.log('RESPONSE STATUS:', res.status);  
  console.log('RESPONSE BODY:', raw);
  if (!res.ok) {
    throw new Error(raw || `HTTP ${res.status}`);
  }

  return raw;
}

async function apiFetchJson(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
  });

  const raw = await res.text();
  let data = null;

  try {
    data = raw ? JSON.parse(raw) : null;
  } catch {
    data = raw;
  }

  if (!res.ok) {
    const message =
      typeof data === 'string'
        ? data
        : data?.message || data?.error || `HTTP ${res.status}`;
    throw new Error(message);
  }

  return data;
}

function text(node, tag) {
  return node.querySelector(tag)?.textContent ?? null;
}

function parsePaymentNode(node) {
  return {
    paymentId: text(node, 'paymentId'),
    status: text(node, 'status'),
    message: text(node, 'message'),
    senderBankId: text(node, 'senderBankId'),
    receiverBankId: text(node, 'receiverBankId'),
    senderAccount: text(node, 'senderAccount'),
    receiverAccount: text(node, 'receiverAccount'),
    amount: text(node, 'amount') ? Number(text(node, 'amount')) : null,
    settledAt: text(node, 'settledAt'),
  };
}

function parsePaymentsXml(xmlString) {
  const xml = new DOMParser().parseFromString(xmlString, 'application/xml');
  return Array.from(xml.querySelectorAll('payments payments'))
    .map(parsePaymentNode);
}

function parsePaymentResponseXml(xmlString) {
  const xml = new DOMParser().parseFromString(xmlString, 'application/xml');
  const root = xml.querySelector('SorbnetPaymentResponse') 
            || xml.querySelector('PaymentResponseDto');
  if (!root) throw new Error('Niepoprawna odpowiedź XML');
  return parsePaymentNode(root);
}

function buildPaymentXml({
  paymentId,
  amount,
  currency = 'PLN',
  senderBankId,
  receiverBankId,
  senderAccount,
  receiverAccount,
  title,
  status = 'NEW',
}) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<SorbnetPaymentRequest>
  <paymentId>${paymentId}</paymentId>
  <amount>${amount}</amount>
  <currency>${currency}</currency>
  <senderBankId>${senderBankId}</senderBankId>
  <receiverBankId>${receiverBankId}</receiverBankId>
  <senderAccount>${senderAccount}</senderAccount>
  <receiverAccount>${receiverAccount}</receiverAccount>
  <title>${title ?? ''}</title>
  <status>${status}</status>
</SorbnetPaymentRequest>`;
}

export async function fetchAccountStatus(bankId) {
  try {
    return await apiFetchJson(`/api/sorbnet/accounts/${bankId}/status`);
  } catch (e) {
    throw new Error(`Błąd pobierania statusu konta: ${e.message}`);
  }
}

export async function simulateDeposit(bankId, amount, sourceBankId = 'NBP') {
  try {
    const params = new URLSearchParams({
      amount: String(amount),
      sourceBankId,
    });

    return await apiFetchJson(`/api/sorbnet/accounts/${bankId}/deposit?${params.toString()}`, {
      method: 'POST',
    });
  } catch (e) {
    throw new Error(`Błąd symulacji wpłaty: ${e.message}`);
  }
}

export async function fetchPayments(bankId, days = 1) {
  try {
    const from = new Date();
    from.setDate(from.getDate() - days + 1);
    from.setHours(0, 0, 0, 0);

    const pad = (n) => String(n).padStart(2, '0');
    const fromStr = `${from.getFullYear()}-${pad(from.getMonth() + 1)}-${pad(from.getDate())}`;

    const xml = await apiFetchText(
      `/api/sorbnet/payments?bankId=${encodeURIComponent(bankId)}&from=${fromStr}`,
      { headers: { Accept: 'application/xml' } }
    );

    return parsePaymentsXml(xml);
  } catch (e) {
    throw new Error(`Błąd pobierania przelewów: ${e.message}`);
  }
}

export async function createPayment({
  paymentId,
  amount,
  currency = 'PLN',
  senderBankId,
  receiverBankId,
  senderAccount,
  receiverAccount,
  title,
  status = 'NEW',
}) {
  try {
    const xmlBody = buildPaymentXml({
      paymentId, amount, currency,
      senderBankId, receiverBankId,
      senderAccount, receiverAccount,
      title, status,
    });

    console.log('SENDING XML:', xmlBody);

    const response = await fetch('http://localhost:8083/api/sorbnet/payments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/xml',
          Accept: 'application/xml',
        },
        body: xmlBody,
      });

      console.log('STATUS:', response.status);
      const text = await response.text();
      console.log('RESPONSE BODY:', text);

      return parsePaymentResponseXml(text);
  } catch (e) {
    throw new Error(`Błąd wysyłania przelewu: ${e.message}`);
  }
}

export function connectWebSocket({
  bankId,
  onPayment,
  onAlert,
  onConnect,
  onDisconnect,
}) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${BASE}/ws`),
    reconnectDelay: 3000,
    onConnect: () => {
      onConnect?.();

      client.subscribe('/topic/payments', (msg) => {
        try {
          const payment = JSON.parse(msg.body);
          if (payment.senderBankId === bankId || payment.receiverBankId === bankId) {
            onPayment?.(payment);
          }
        } catch (e) {
          console.error('[WS] payment parse error:', e);
        }
      });

      client.subscribe(`/topic/alerts/${bankId}`, (msg) => {
        try {
          onAlert?.(JSON.parse(msg.body));
        } catch (e) {
          console.error('[WS] alert parse error:', e);
        }
      });

      client.publish({ destination: `/app/alerts/${bankId}`, body: '' });
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: (frame) => console.error('[WS] STOMP error:', frame),
  });

  client.activate();
  return client;
}

export async function fetchBanks() {
  try {
    return await apiFetchJson('/api/sorbnet/accounts');
  } catch (e) {
    throw new Error(`Błąd pobierania listy banków: ${e.message}`);
  }
}
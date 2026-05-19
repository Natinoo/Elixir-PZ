import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const SORBNET_URL = 'http://localhost:8083';

export async function fetchAccountStatus(bankId) {
  const res = await fetch(`${SORBNET_URL}/api/accounts/${bankId}`);
  if (!res.ok) throw new Error('Błąd pobierania statusu konta');
  return res.json();
}

// days: 0 = dziś, 7 = tydzień, 30 = miesiąc
export async function fetchPayments(bankId, days = 0) {
  const from = new Date();
  from.setDate(from.getDate() - days);
  from.setHours(0, 0, 0, 0);
  const fromStr = from.toISOString().slice(0, 10); // YYYY-MM-DD

  const res = await fetch(
    `http://localhost:8083/api/sorbnet/payments?bankId=${bankId}&from=${fromStr}`
  );
  if (!res.ok) throw new Error('Błąd pobierania przelewów');
  return res.json();
}

export async function simulateDeposit(bankId, amount) {
  const res = await fetch(`${SORBNET_URL}/api/operator/deposit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetBankId: bankId, amount }),
  });
  if (!res.ok) throw new Error('Błąd symulacji wpłaty');
  return res.json();
}

export function connectWebSocket({ bankId, onPayment, onAlert, onConnect, onDisconnect }) {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${SORBNET_URL}/ws`),
    reconnectDelay: 3000,
    onConnect: () => {
      onConnect?.();

      // wszystkie przelewy — filtrujemy po stronie GUI
      client.subscribe('/topic/payments', (msg) => {
        const payment = JSON.parse(msg.body);
        if (payment.senderBankId === bankId || payment.receiverBankId === bankId) {
          onPayment?.(payment);
        }
      });

      // alerty tylko dla tego banku
      client.subscribe(`/topic/alerts/${bankId}`, (msg) => {
        onAlert?.(JSON.parse(msg.body));
      });

      // zapytaj o aktualny stan przy podłączeniu
      client.publish({ destination: `/app/alerts/${bankId}`, body: '' });
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: (frame) => console.error('[WS] STOMP error:', frame),
  });

  client.activate();
  return client;
}
// src/components/PaymentsList.jsx
import { useState, useEffect } from 'react';
import { fetchPayments } from '../api/sorbnetApi';

export default function PaymentsList({ bankId }) {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [daysBack, setDaysBack] = useState(1);

  useEffect(() => {
    setLoading(true);
    fetchPayments(bankId, daysBack)
      .then(setPayments)
      .catch(() => setPayments([]))
      .finally(() => setLoading(false));
  }, [bankId, daysBack]);

  const fmt = n =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n ?? 0);

  const STATUS = {
    SETTLED:      { label: 'Rozliczony',  cls: 'badge-success' },
    PENDING:      { label: 'Oczekujący',  cls: 'badge-neutral' },
    GRIDLOCK_HELD:{ label: 'Gridlock',    cls: 'badge-warning' },
    REJECTED:     { label: 'Odrzucony',   cls: 'badge-error'   },
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
        <h2 style={{ fontSize: '1rem', fontWeight: 600 }}>Historia przelewów</h2>
        <select
          value={daysBack}
          onChange={e => setDaysBack(Number(e.target.value))}
          style={{
            padding: 'var(--space-2) var(--space-3)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--color-border)',
            background: 'var(--color-surface)',
            font: 'inherit',
            fontSize: '0.875rem',
          }}
        >
          <option value={1}>Dzisiaj</option>
          <option value={7}>Ostatnie 7 dni</option>
          <option value={14}>Ostatnie 14 dni</option>
          <option value={30}>Ostatnie 30 dni</option>
        </select>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? (
          <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie…</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Kierunek</th>
                <th>Kontrahent</th>
                <th>Kwota</th>
                <th>Tytuł</th>
                <th>Status</th>
                <th>Data</th>
              </tr>
            </thead>
            <tbody>
              {payments.map(p => {
                const isOut = p.senderBankId === bankId;
                const s = STATUS[p.status] ?? { label: p.status, cls: 'badge-neutral' };
                return (
                  <tr key={p.paymentId}>
                    <td>
                      <span className={`badge ${isOut ? 'badge-error' : 'badge-success'}`}>
                        {isOut ? '↑ Wychodzący' : '↓ Przychodzący'}
                      </span>
                    </td>
                    <td>{isOut ? p.receiverBankId : p.senderBankId}</td>
                    <td style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                      {isOut ? '−' : '+'}{fmt(p.amount)}
                    </td>
                    <td style={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--color-text-muted)' }}>
                      {p.title}
                    </td>
                    <td><span className={`badge ${s.cls}`}>{s.label}</span></td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                      {p.settledAt
                        ? new Date(p.settledAt).toLocaleString('pl-PL')
                        : new Date(p.createdAt).toLocaleString('pl-PL')}
                    </td>
                  </tr>
                );
              })}
              {payments.length === 0 && (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--color-text-muted)' }}>
                    Brak przelewów w wybranym okresie
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
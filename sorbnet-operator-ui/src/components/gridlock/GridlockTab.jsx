import { useState, useEffect } from 'react';
import { fetchGridlock } from '../../api/sorbnetApi';

export default function GridlockTab() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchGridlock().then(setPayments).finally(() => setLoading(false));
    const interval = setInterval(() => fetchGridlock().then(setPayments), 15000);
    return () => clearInterval(interval);
  }, []);

  const fmt = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);

  return (
    <div>
      <h1 className="page-title">Kolejka Gridlock</h1>
      <p style={{ color: 'var(--color-text-muted)', marginBottom: 'var(--space-6)', fontSize: '0.875rem' }}>
        Przelewy wstrzymane przez mechanizm gridlock resolution. Odświeżanie co 15s.
      </p>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie...</p> : (
          <table>
            <thead>
              <tr><th>Z banku</th><th>Do banku</th><th>Kwota</th><th>Tytuł</th></tr>
            </thead>
            <tbody>
              {payments.map(p => (
                <tr key={p.paymentId}>
                  <td>{p.senderBankId}</td>
                  <td>{p.receiverBankId}</td>
                  <td style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(p.amount)}</td>
                  <td>{p.title}</td>
                </tr>
              ))}
              {payments.length === 0 && (
                <tr><td colSpan={4} style={{ textAlign: 'center', padding: '2rem', color: 'var(--color-text-muted)' }}>Kolejka gridlock jest pusta</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
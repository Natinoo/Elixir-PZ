import { useState, useEffect } from 'react';
import { fetchPayments } from '../../api/sorbnetApi';

const STATUS_LABELS = {
  SETTLED: { label: 'Rozliczony', cls: 'badge-success' },
  PENDING: { label: 'Oczekujący', cls: 'badge-neutral' },
  GRIDLOCK_HELD: { label: 'Gridlock', cls: 'badge-warning' },
  REJECTED: { label: 'Odrzucony', cls: 'badge-error' },
};

export default function PaymentsTab() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterBank, setFilterBank] = useState('');

  const load = () => {
    const params = {};
    if (filterStatus) params.status = filterStatus;
    if (filterBank) params.bankId = filterBank;
    fetchPayments(params).then(setPayments).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [filterStatus, filterBank]);

  const fmt = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);
  const fmtDate = (d) => d ? new Date(d).toLocaleString('pl-PL') : '—';

  return (
    <div>
      <h1 className="page-title">Historia transakcji</h1>
      <div style={{ display: 'flex', gap: 'var(--space-4)', marginBottom: 'var(--space-6)' }}>
        <select
          value={filterStatus}
          onChange={e => setFilterStatus(e.target.value)}
          style={{ padding: 'var(--space-2) var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', background: 'var(--color-surface)', font: 'inherit', fontSize: '0.875rem' }}
        >
          <option value="">Wszystkie statusy</option>
          <option value="SETTLED">Rozliczone</option>
          <option value="PENDING">Oczekujące</option>
          <option value="GRIDLOCK_HELD">Gridlock</option>
          <option value="REJECTED">Odrzucone</option>
        </select>
        <input
          placeholder="Filtruj po banku (np. PKO)"
          value={filterBank}
          onChange={e => setFilterBank(e.target.value)}
          style={{ padding: 'var(--space-2) var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', background: 'var(--color-surface)', font: 'inherit', fontSize: '0.875rem', width: '200px' }}
        />
      </div>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie...</p> : (
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Z banku</th><th>Do banku</th><th>Kwota</th><th>Tytuł</th><th>Status</th><th>Data</th>
              </tr>
            </thead>
            <tbody>
              {payments.map(p => {
                const s = STATUS_LABELS[p.status] || { label: p.status, cls: 'badge-neutral' };
                return (
                  <tr key={p.paymentId}>
                    <td style={{ color: 'var(--color-text-muted)', fontSize: '0.75rem' }}>{p.paymentId?.slice(0, 8)}…</td>
                    <td>{p.senderBankId}</td>
                    <td>{p.receiverBankId}</td>
                    <td style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(p.amount)}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.title}</td>
                    <td><span className={`badge ${s.cls}`}>{s.label}</span></td>
                    <td style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>{fmtDate(p.settledAt || p.createdAt)}</td>
                  </tr>
                );
              })}
              {payments.length === 0 && (
                <tr><td colSpan={7} style={{ textAlign: 'center', padding: '2rem', color: 'var(--color-text-muted)' }}>Brak transakcji</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
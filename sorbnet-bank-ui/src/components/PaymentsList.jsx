export default function PaymentsList({ bankId, payments = [] }) {
  const fmt = (n) =>
    new Intl.NumberFormat('pl-PL', {
      style: 'currency',
      currency: 'PLN',
    }).format(n ?? 0);

  const fmtDate = (d) => (d ? new Date(d).toLocaleString('pl-PL') : '—');

  const STATUS = {
    SETTLED: { label: 'Rozliczony', cls: 'badge-success' },
    GRIDLOCK_HELD: { label: 'Gridlock', cls: 'badge-warning' },
    REJECTED: { label: 'Odrzucony', cls: 'badge-error' },
  };

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <table>
        <thead>
          <tr>
            <th>Kierunek</th>
            <th>Kontrahent</th>
            <th>Kwota</th>
            <th>Status</th>
            <th>Data rozliczenia</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((p) => {
            const isOut = p.senderBankId === bankId;
            const status = STATUS[p.status] ?? { label: p.status, cls: 'badge-neutral' };

            return (
              <tr key={p.paymentId}>
                <td>
                  <span className={`badge ${isOut ? 'badge-error' : 'badge-success'}`}>
                    {isOut ? '↑ Wychodzący' : '↓ Przychodzący'}
                  </span>
                </td>
                <td>{isOut ? p.receiverBankId : p.senderBankId}</td>
                <td style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                  {isOut ? '−' : '+'}
                  {fmt(p.amount)}
                </td>
                <td>
                  <span className={`badge ${status.cls}`}>{status.label}</span>
                </td>
                <td style={{ fontSize: '0.82rem', color: 'var(--color-text-muted)' }}>
                  {fmtDate(p.settledAt)}
                </td>
              </tr>
            );
          })}

          {payments.length === 0 && (
            <tr>
              <td
                colSpan={5}
                style={{
                  textAlign: 'center',
                  padding: '2rem',
                  color: 'var(--color-text-muted)',
                }}
              >
                Brak przelewów w wybranym okresie
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
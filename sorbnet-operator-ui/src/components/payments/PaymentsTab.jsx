import { useEffect, useMemo, useState } from 'react';
import { fetchPayments, fetchSettledToday } from '../../api/sorbnetApi';

const STATUS_LABELS = {
  SETTLED: { label: 'Rozliczony', cls: 'badge-success' },
  PENDING: { label: 'Oczekujący', cls: 'badge-neutral' },
  GRIDLOCK_HELD: { label: 'Gridlock', cls: 'badge-warning' },
  REJECTED: { label: 'Odrzucony', cls: 'badge-error' },
};

export default function PaymentsTab() {
  const [payments, setPayments] = useState([]);
  const [settledToday, setSettledToday] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterBank, setFilterBank] = useState('');
  const [search, setSearch] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const params = {};
      if (filterStatus) params.status = filterStatus;
      if (filterBank) params.bankId = filterBank.trim().toUpperCase();

      const [paymentsData, settledTodayData] = await Promise.all([
        fetchPayments(params),
        fetchSettledToday(),
      ]);

      setPayments(Array.isArray(paymentsData) ? paymentsData : []);
      setSettledToday(Array.isArray(settledTodayData) ? settledTodayData : []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [filterStatus, filterBank]);

  const fmtMoney = (amount, currency = 'PLN') =>
    new Intl.NumberFormat('pl-PL', {
      style: 'currency',
      currency: currency || 'PLN',
      maximumFractionDigits: 2,
    }).format(Number(amount || 0));

  const fmtDate = (date) => (date ? new Date(date).toLocaleString('pl-PL') : '—');

  const shortId = (id) => {
    if (!id) return '—';
    return id.length > 16 ? `${id.slice(0, 16)}…` : id;
  };

  const visiblePayments = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return payments;

    return payments.filter((p) => {
      return [
        p.paymentId,
        p.senderBankId,
        p.receiverBankId,
        p.senderAccount,
        p.receiverAccount,
        p.title,
        p.currency,
        p.rejectReason,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(q));
    });
  }, [payments, search]);

  const summary = useMemo(() => {
    const settledCount = visiblePayments.filter((p) => p.status === 'SETTLED').length;
    const rejectedCount = visiblePayments.filter((p) => p.status === 'REJECTED').length;
    const gridlockCount = visiblePayments.filter((p) => p.status === 'GRIDLOCK_HELD').length;

    const totalAmount = visiblePayments.reduce(
      (sum, p) => sum + Number(p.amount || 0),
      0
    );

    const settledTodayAmount = settledToday.reduce(
      (sum, p) => sum + Number(p.amount || 0),
      0
    );

    return {
      totalCount: visiblePayments.length,
      settledCount,
      rejectedCount,
      gridlockCount,
      totalAmount,
      settledTodayCount: settledToday.length,
      settledTodayAmount,
    };
  }, [visiblePayments, settledToday]);

  return (
    <div>
      <h1 className="page-title">Historia transakcji</h1>

      <div className="kpi-grid">
        <div className="card kpi-card">
          <span className="kpi-label">Widoczne przelewy</span>
          <strong className="kpi-value">{summary.totalCount}</strong>
        </div>
        <div className="card kpi-card">
          <span className="kpi-label">Łączna kwota</span>
          <strong className="kpi-value">{fmtMoney(summary.totalAmount)}</strong>
        </div>
        <div className="card kpi-card">
          <span className="kpi-label">Rozliczone dziś</span>
          <strong className="kpi-value" style={{ color: 'var(--color-success)' }}>
            {summary.settledTodayCount}
          </strong>
        </div>
        <div className="card kpi-card">
          <span className="kpi-label">Kwota rozliczona dziś</span>
          <strong className="kpi-value">{fmtMoney(summary.settledTodayAmount)}</strong>
        </div>
        <div className="card kpi-card">
          <span className="kpi-label">Gridlock</span>
          <strong className="kpi-value" style={{ color: 'var(--color-warning)' }}>
            {summary.gridlockCount}
          </strong>
        </div>
        <div className="card kpi-card">
          <span className="kpi-label">Odrzucone</span>
          <strong className="kpi-value" style={{ color: 'var(--color-error)' }}>
            {summary.rejectedCount}
          </strong>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 'var(--space-6)' }}>
        <div
          style={{
            display: 'flex',
            gap: 'var(--space-4)',
            flexWrap: 'wrap',
            alignItems: 'end',
          }}
        >
          <div style={{ minWidth: '220px' }}>
            <label className="field-label">Status</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="input"
            >
              <option value="">Wszystkie statusy</option>
              <option value="SETTLED">Rozliczone</option>
              <option value="GRIDLOCK_HELD">Gridlock</option>
              <option value="REJECTED">Odrzucone</option>
            </select>
          </div>

          <div style={{ minWidth: '220px' }}>
            <label className="field-label">Bank</label>
            <input
              className="input"
              placeholder="np. PKO"
              value={filterBank}
              onChange={(e) => setFilterBank(e.target.value)}
            />
          </div>

          <div style={{ minWidth: '280px', flex: '1 1 320px' }}>
            <label className="field-label">Szukaj w danych przelewu</label>
            <input
              className="input"
              placeholder="ID, rachunek, tytuł, waluta, powód odrzucenia..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <button className="btn btn-secondary" onClick={load}>
            Odśwież
          </button>
        </div>
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? (
          <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie...</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Z banku</th>
                <th>Do banku</th>
                <th>Rachunek nadawcy</th>
                <th>Rachunek odbiorcy</th>
                <th>Kwota</th>
                <th>Waluta</th>
                <th>Tytuł</th>
                <th>Status</th>
                <th>Utworzono</th>
                <th>Rozliczono</th>
                <th>Powód odrzucenia</th>
              </tr>
            </thead>
            <tbody>
              {visiblePayments.map((p) => {
                const s = STATUS_LABELS[p.status] || {
                  label: p.status || 'Nieznany',
                  cls: 'badge-neutral',
                };

                return (
                  <tr key={p.paymentId}>
                    <td
                      title={p.paymentId}
                      style={{
                        color: 'var(--color-text-muted)',
                        fontSize: '0.75rem',
                        fontVariantNumeric: 'tabular-nums',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {shortId(p.paymentId)}
                    </td>

                    <td>
                      <strong>{p.senderBankId || '—'}</strong>
                    </td>

                    <td>
                      <strong>{p.receiverBankId || '—'}</strong>
                    </td>

                    <td
                      style={{
                        fontVariantNumeric: 'tabular-nums',
                        fontSize: '0.8rem',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {p.senderAccount || '—'}
                    </td>

                    <td
                      style={{
                        fontVariantNumeric: 'tabular-nums',
                        fontSize: '0.8rem',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {p.receiverAccount || '—'}
                    </td>

                    <td style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                      {fmtMoney(p.amount, p.currency || 'PLN')}
                    </td>

                    <td>{p.currency || 'PLN'}</td>

                    <td
                      title={p.title}
                      style={{
                        minWidth: '220px',
                        maxWidth: '260px',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {p.title || '—'}
                    </td>

                    <td>
                      <span className={`badge ${s.cls}`}>{s.label}</span>
                    </td>

                    <td
                      style={{
                        color: 'var(--color-text-muted)',
                        fontSize: '0.8rem',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {fmtDate(p.createdAt)}
                    </td>

                    <td
                      style={{
                        color: 'var(--color-text-muted)',
                        fontSize: '0.8rem',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {fmtDate(p.settledAt)}
                    </td>

                    <td
                      title={p.rejectReason}
                      style={{
                        color: p.rejectReason ? 'var(--color-error)' : 'var(--color-text-muted)',
                        minWidth: '220px',
                        maxWidth: '320px',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        fontSize: '0.8rem',
                      }}
                    >
                      {p.rejectReason || '—'}
                    </td>
                  </tr>
                );
              })}

              {visiblePayments.length === 0 && (
                <tr>
                  <td
                    colSpan={12}
                    style={{
                      textAlign: 'center',
                      padding: '2rem',
                      color: 'var(--color-text-muted)',
                    }}
                  >
                    Brak transakcji
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
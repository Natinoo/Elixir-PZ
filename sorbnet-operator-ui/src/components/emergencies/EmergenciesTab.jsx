import { useEffect, useMemo, useState } from 'react';
import { fetchEmergencies } from '../../api/sorbnetApi';
import { useWebSocket } from '../../hooks/useWebSocket';

export default function EmergenciesTab({ onCountChange }) {
  const [banks, setBanks] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try {
      const data = await fetchEmergencies();
      const list = Array.isArray(data) ? data : [];
      setBanks(list);
      onCountChange?.(list.length);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useWebSocket(['/topic/operator/emergencies'], () => {
    load();
  });

  const fmtMoney = (n) =>
    new Intl.NumberFormat('pl-PL', {
      style: 'currency',
      currency: 'PLN',
      maximumFractionDigits: 2,
    }).format(Number(n || 0));

  const fmtDate = (d) => (d ? new Date(d).toLocaleString('pl-PL') : '—');

  const getOverlimitAmount = (balance, debtLimit) => {
    const absBalance = Math.abs(Number(balance || 0));
    const limit = Number(debtLimit || 0);
    if (Number(balance || 0) >= 0 || absBalance <= limit) return 0;
    return absBalance - limit;
  };

  const getDurationLabel = (date) => {
    if (!date) return '—';
    const start = new Date(date).getTime();
    const diffMs = Date.now() - start;
    const totalMinutes = Math.max(0, Math.floor(diffMs / 60000));

    const days = Math.floor(totalMinutes / 1440);
    const hours = Math.floor((totalMinutes % 1440) / 60);
    const minutes = totalMinutes % 60;

    if (days > 0) return `${days}d ${hours}h ${minutes}m`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes} min`;
  };

  const getPriority = (bank) => {
    if (bank.blocked) return 'KRYTYCZNY';
    const over = getOverlimitAmount(bank.balance, bank.debtLimit);
    if (over > 10000000) return 'WYSOKI';
    if (over > 0) return 'PODWYŻSZONY';
    return 'MONITORING';
  };

  const getPriorityClass = (bank) => {
    if (bank.blocked) return 'badge-error';
    const over = getOverlimitAmount(bank.balance, bank.debtLimit);
    if (over > 10000000) return 'badge-error';
    if (over > 0) return 'badge-warning';
    return 'badge-neutral';
  };

  const sortedBanks = useMemo(() => {
    return [...banks].sort((a, b) => {
      if (a.blocked !== b.blocked) return a.blocked ? -1 : 1;

      const overA = getOverlimitAmount(a.balance, a.debtLimit);
      const overB = getOverlimitAmount(b.balance, b.debtLimit);
      if (overA !== overB) return overB - overA;

      const timeA = a.overlimitSince ? new Date(a.overlimitSince).getTime() : Infinity;
      const timeB = b.overlimitSince ? new Date(b.overlimitSince).getTime() : Infinity;
      return timeA - timeB;
    });
  }, [banks]);

  const summary = useMemo(() => {
    const blockedCount = banks.filter((b) => b.blocked).length;
    const overlimitCount = banks.filter((b) => !b.blocked).length;
    const totalShortage = banks.reduce(
      (sum, b) => sum + getOverlimitAmount(b.balance, b.debtLimit),
      0
    );

    return {
      total: banks.length,
      blockedCount,
      overlimitCount,
      totalShortage,
    };
  }, [banks]);

  return (
    <div>
      <h1 className="page-title">Sytuacje nadzwyczajne</h1>

      {!loading && banks.length > 0 && (
        <div className="kpi-grid">
          <div className="card kpi-card">
            <span className="kpi-label">Banki alarmowe</span>
            <strong className="kpi-value">{summary.total}</strong>
          </div>
          <div className="card kpi-card">
            <span className="kpi-label">Ponad limit</span>
            <strong className="kpi-value" style={{ color: 'var(--color-warning)' }}>
              {summary.overlimitCount}
            </strong>
          </div>
          <div className="card kpi-card">
            <span className="kpi-label">Zablokowane</span>
            <strong className="kpi-value" style={{ color: 'var(--color-error)' }}>
              {summary.blockedCount}
            </strong>
          </div>
          <div className="card kpi-card">
            <span className="kpi-label">Łączny niedobór płynności</span>
            <strong className="kpi-value">{fmtMoney(summary.totalShortage)}</strong>
          </div>
        </div>
      )}

      {banks.length === 0 && !loading && (
        <div
          className="card"
          style={{
            textAlign: 'center',
            padding: '3rem',
            color: 'var(--color-text-muted)',
          }}
        >
          <p style={{ fontSize: '2rem', marginBottom: '1rem' }}>✓</p>
          <p>Brak aktywnych sytuacji nadzwyczajnych. Wszystkie banki utrzymują płynność.</p>
        </div>
      )}

      {loading && (
        <div className="card">
          <p style={{ color: 'var(--color-text-muted)' }}>Ładowanie sytuacji nadzwyczajnych...</p>
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
        {sortedBanks.map((b) => {
          const shortage = getOverlimitAmount(b.balance, b.debtLimit);

          return (
            <div
              key={b.bankId}
              className="card"
              style={{
                background: b.blocked
                  ? 'var(--color-error-highlight)'
                  : 'var(--color-warning-highlight)',
                borderColor: b.blocked
                  ? 'var(--color-error)'
                  : 'var(--color-warning)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                  gap: 'var(--space-4)',
                  flexWrap: 'wrap',
                }}
              >
                <div style={{ flex: '1 1 520px' }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.6rem',
                      flexWrap: 'wrap',
                      marginBottom: '0.5rem',
                    }}
                  >
                    <strong style={{ fontSize: '1.05rem' }}>{b.bankId}</strong>
                    <span style={{ color: 'var(--color-text-muted)' }}>{b.bankName}</span>
                    <span className={`badge ${getPriorityClass(b)}`}>{getPriority(b)}</span>
                  </div>

                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                      gap: '0.75rem 1rem',
                    }}
                  >
                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Saldo</div>
                      <div style={{ fontWeight: 700, color: Number(b.balance) < 0 ? 'var(--color-error)' : 'inherit' }}>
                        {fmtMoney(b.balance)}
                      </div>
                    </div>

                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Limit zadłużenia</div>
                      <div style={{ fontWeight: 700 }}>{fmtMoney(b.debtLimit)}</div>
                    </div>

                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Przekroczenie limitu</div>
                      <div style={{ fontWeight: 700, color: shortage > 0 ? 'var(--color-error)' : 'inherit' }}>
                        {shortage > 0 ? fmtMoney(shortage) : '—'}
                      </div>
                    </div>

                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Ponad limit od</div>
                      <div style={{ fontWeight: 600 }}>{fmtDate(b.overlimitSince)}</div>
                    </div>

                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Czas trwania alarmu</div>
                      <div style={{ fontWeight: 600 }}>{getDurationLabel(b.overlimitSince)}</div>
                    </div>

                    <div>
                      <div className="text-muted" style={{ fontSize: '0.8rem' }}>Blokada od</div>
                      <div style={{ fontWeight: 600 }}>{fmtDate(b.blockedAt || b.blockedSince)}</div>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', alignItems: 'flex-end' }}>
                  <span className={`badge ${b.blocked ? 'badge-error' : 'badge-warning'}`}>
                    {b.blocked ? 'ZABLOKOWANY' : 'PONAD LIMIT'}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
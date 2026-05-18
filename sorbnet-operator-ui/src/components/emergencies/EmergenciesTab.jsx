import { useState, useEffect } from 'react';
import { fetchEmergencies } from '../../api/sorbnetApi';
import { useWebSocket } from '../../hooks/useWebSocket';

export default function EmergenciesTab({ onCountChange }) {
  const [banks, setBanks] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = () => fetchEmergencies().then(data => {
    setBanks(data);
    onCountChange(data.length);
  }).finally(() => setLoading(false));

  useEffect(() => { load(); }, []);

  useWebSocket(['/topic/operator/emergencies'], (topic, msg) => {
    load(); // odśwież listę przy każdym evencie WebSocket
  });

  const fmt = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);

  return (
    <div>
      <h1 className="page-title">Sytuacje nadzwyczajne</h1>
      {banks.length === 0 && !loading && (
        <div className="card" style={{ textAlign: 'center', padding: '3rem', color: 'var(--color-text-muted)' }}>
          <p style={{ fontSize: '2rem', marginBottom: '1rem' }}>✓</p>
          <p>Wszystkie banki mają płynność finansową</p>
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-4)' }}>
        {banks.map(b => (
          <div key={b.bankId} className="card" style={{ background: 'var(--color-warning-highlight)', borderColor: 'var(--color-warning)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div>
                <strong>{b.bankId}</strong> — {b.bankName}
                <div style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', marginTop: 'var(--space-1)' }}>
                  Saldo: {fmt(b.balance)} | Limit: {fmt(b.debtLimit)}
                </div>
                {b.overlimitSince && (
                  <div style={{ fontSize: '0.8rem', color: 'var(--color-warning)', marginTop: 'var(--space-1)' }}>
                    Ponad limit od: {new Date(b.overlimitSince).toLocaleString('pl-PL')}
                  </div>
                )}
              </div>
              <span className={`badge ${b.blocked ? 'badge-error' : 'badge-warning'}`}>
                {b.blocked ? 'ZABLOKOWANY' : 'PONAD LIMIT'}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
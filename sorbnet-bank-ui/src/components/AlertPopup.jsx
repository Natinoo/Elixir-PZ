import { useEffect } from 'react';

export default function AlertPopup({ alert, onClose }) {
  useEffect(() => {
    const t = setTimeout(onClose, 10000);
    return () => clearTimeout(t);
  }, [alert]);

  if (!alert) return null;

  return (
    <div className="alert-popup">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 'var(--space-4)' }}>
        <div>
          <strong style={{ display: 'block', marginBottom: 'var(--space-1)' }}>⚠ {alert.type === 'BANK_BLOCKED' ? 'Bank zablokowany' : 'Przekroczono limit zadłużenia'}</strong>
          <p style={{ fontSize: '0.875rem', opacity: 0.9 }}>{alert.message}</p>
          {alert.balance !== undefined && (
            <p style={{ fontSize: '0.8rem', marginTop: 'var(--space-2)', opacity: 0.8 }}>
              Saldo: {new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(alert.balance)} | Limit: {new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(alert.debtLimit)}
            </p>
          )}
        </div>
        <button onClick={onClose} style={{ color: '#fff', opacity: 0.8, fontSize: '1.2rem', lineHeight: 1, padding: 0 }}>×</button>
      </div>
    </div>
  );
}
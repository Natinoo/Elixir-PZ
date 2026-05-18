import { useCountdown } from '../hooks/useCountdown';

export default function OverlimitCountdown({ overlimitSince }) {
  const remaining = useCountdown(overlimitSince);

  if (remaining === null) return null;

  const totalSec = Math.floor(remaining / 1000);
  const h = Math.floor(totalSec / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;

  const isUrgent = remaining < 30 * 60 * 1000; // < 30 min
  const isExpired = remaining === 0;

  return (
    <div style={{
      display: 'flex',
      alignItems: 'center',
      gap: 'var(--space-3)',
      padding: 'var(--space-3) var(--space-4)',
      borderRadius: 'var(--radius-md)',
      background: isExpired ? 'var(--color-error-highlight)' : isUrgent ? 'var(--color-warning-highlight)' : 'var(--color-surface-offset)',
      border: `1px solid ${isExpired ? 'var(--color-error)' : isUrgent ? 'var(--color-warning)' : 'var(--color-border)'}`,
      fontSize: '0.875rem',
    }}>
      <span style={{ fontSize: '1rem' }}>{isExpired ? '🔴' : isUrgent ? '⚠' : '⏳'}</span>
      <span>
        {isExpired
          ? 'Czas na uzupełnienie środków minął — bank zostanie zablokowany'
          : <>Do auto-blokady: <strong style={{
              fontVariantNumeric: 'tabular-nums',
              color: isUrgent ? 'var(--color-warning)' : 'var(--color-text)',
            }}>
              {String(h).padStart(2, '0')}:{String(m).padStart(2, '0')}:{String(s).padStart(2, '0')}
            </strong></>
        }
      </span>
    </div>
  );
}
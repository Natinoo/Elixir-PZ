import { useEffect, useState, useCallback } from 'react';
import { connectWebSocket, fetchPayments, fetchAccountStatus, simulateDeposit } from '../api/sorbnetApi';
import PaymentsList from './PaymentsList';
import AlertPopup from './AlertPopup';
import OverlimitCountdown from './OverlimitCountdown';

const PERIODS = [
  { label: 'Dziś',    days: 0 },
  { label: '7 dni',   days: 7 },
  { label: '30 dni',  days: 30 },
];

export default function Dashboard({ bankId }) {
  const [account, setAccount]           = useState(null);
  const [payments, setPayments]         = useState([]);
  const [alert, setAlert]               = useState(null);
  const [period, setPeriod]             = useState(0);       // dni historii
  const [depositAmount, setDepositAmount] = useState('');
  const [depositLoading, setDepositLoading] = useState(false);
  const [connected, setConnected]       = useState(false);
  const [gridlockActive, setGridlockActive] = useState(false);

  const refreshAccount = useCallback(() =>
    fetchAccountStatus(bankId).then(setAccount).catch(console.error),
  [bankId]);

  const refreshPayments = useCallback((days) =>
    fetchPayments(bankId, days).then(setPayments).catch(console.error),
  [bankId]);

  // inicjalne załadowanie
  useEffect(() => {
    refreshAccount();
    refreshPayments(period);
  }, [bankId]);

  // zmiana okresu historii
  useEffect(() => {
    refreshPayments(period);
  }, [period]);

  // WebSocket
  useEffect(() => {
    const client = connectWebSocket({
      bankId,
      onPayment: (payment) => {
        // wstaw lub zaktualizuj na liście
        setPayments((prev) => {
          const exists = prev.some(p => p.paymentId === payment.paymentId);
          return exists
            ? prev.map(p => p.paymentId === payment.paymentId ? payment : p)
            : [payment, ...prev];
        });
        refreshAccount();
        // wyczyść gridlock jeśli płatność przeszła
        if (payment.status === 'SETTLED') setGridlockActive(false);
      },
      onAlert: (alertData) => {
        if (alertData.type === 'DEBT_LIMIT_EXCEEDED') {
          setAlert(alertData);
          setGridlockActive(true);
        } else if (alertData.type === 'APPROACHING_DEBT_LIMIT') {
          setAlert(alertData);
        } else if (alertData.alert === false) {
          // saldo OK — ukryj alert jeśli był
          setAlert(null);
          setGridlockActive(false);
        }
        refreshAccount();
      },
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
    });
    return () => client.deactivate();
  }, [bankId]);

  const handleDeposit = async () => {
    if (!depositAmount || isNaN(depositAmount)) return;
    setDepositLoading(true);
    try {
      await simulateDeposit(bankId, parseFloat(depositAmount));
      setDepositAmount('');
      const updated = await fetchAccountStatus(bankId);
      setAccount(updated);
      if (updated.balance >= -updated.debtLimit) {
        setAlert(null);
        setGridlockActive(false);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setDepositLoading(false);
    }
  };

  const debtUsedPct   = account ? Math.min(100, Math.max(0, (-account.balance / account.debtLimit) * 100)) : 0;
  const isOverlimit   = account && account.balance < -account.debtLimit;
  const isApproaching = account && !isOverlimit && account.balance < -(account.debtLimit * 0.8);

  return (
    <div className="dashboard">
      {/* Header */}
      <header className="dash-header">
        <div className="dash-header__left">
          <svg className="dash-logo" viewBox="0 0 32 32" fill="none" aria-label="SORBNET">
            <rect x="2" y="14" width="28" height="4" fill="currentColor" opacity="0.9"/>
            <rect x="6"  y="8"  width="4" height="16" fill="currentColor"/>
            <rect x="14" y="8"  width="4" height="16" fill="currentColor"/>
            <rect x="22" y="8"  width="4" height="16" fill="currentColor"/>
            <rect x="2"  y="22" width="28" height="2"  fill="currentColor" opacity="0.5"/>
          </svg>
          <span className="dash-header__title">SORBNET RTGS — Panel banku</span>
          <span className="dash-header__bank">{bankId}</span>
        </div>
        <span className={`ws-badge ${connected ? 'ws-badge--on' : 'ws-badge--off'}`}>
          <span className="ws-badge__dot" />
          {connected ? 'Live' : 'Łączenie…'}
        </span>
      </header>

      <main className="dash-main">
        {/* KPI */}
        <section className="kpi-row">
          <div className={`kpi-card ${isOverlimit ? 'kpi-card--danger' : isApproaching ? 'kpi-card--warn' : ''}`}>
            <span className="kpi-card__label">Saldo rozrachunkowe</span>
            <span className="kpi-card__value">{account ? fmt(account.balance) : '—'}</span>
            <div className="debt-bar">
              <div
                className={`debt-bar__fill ${isOverlimit ? 'debt-bar__fill--danger' : isApproaching ? 'debt-bar__fill--warn' : ''}`}
                style={{ width: `${debtUsedPct}%` }}
              />
            </div>
            <span className="kpi-card__sub">
              Limit zadłużenia: {account ? fmt(account.debtLimit) : '—'}
              {' · '}
              Wykorzystanie: {debtUsedPct.toFixed(1)}%
            </span>
          </div>

          <div className="kpi-card">
            <span className="kpi-card__label">Dostępny kredyt</span>
            <span className="kpi-card__value">{account ? fmt(account.availableCredit) : '—'}</span>
            <span className="kpi-card__sub">{account?.blocked ? '🔒 Bank zablokowany' : '✓ Bank aktywny'}</span>
          </div>

          <div className="kpi-card">
            <span className="kpi-card__label">Min. wpłata do przywrócenia</span>
            <span className={`kpi-card__value ${account?.minDepositToRestore > 0 ? 'kpi-card__value--warn' : ''}`}>
              {account ? fmt(account.minDepositToRestore) : '—'}
            </span>
            <span className="kpi-card__sub">
              {account?.minDepositToRestore > 0
                ? 'Wymagana wpłata aby zmieścić się w limicie'
                : 'Saldo w normie'}
            </span>
          </div>

          <div className="kpi-card">
            <span className="kpi-card__label">Transakcje ({PERIODS.find(p=>p.days===period)?.label})</span>
            <span className="kpi-card__value">{payments.length}</span>
            <span className="kpi-card__sub">
              Rozliczone: {payments.filter(p => p.status === 'SETTLED').length}
              {' / '}
              Wstrzymane: {payments.filter(p => p.status === 'GRIDLOCK_HELD').length}
              {' / '}
              Odrzucone: {payments.filter(p => p.status === 'REJECTED').length}
            </span>
          </div>
        </section>

        {/* Gridlock resolution info */}
        {gridlockActive && (
          <div className="gridlock-banner">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
            </svg>
            <span>
              Uruchomiono <strong>gridlock resolution</strong> — system próbuje automatycznie rozliczyć wstrzymane przelewy.
              Jeśli problem nie zostanie rozwiązany, uzupełnij środki ręcznie.
            </span>
          </div>
        )}

        {/* Odliczanie blokady */}
        {account?.overlimitSince && !account?.blocked && (
          <OverlimitCountdown overlimitSince={account.overlimitSince} />
        )}

        {/* Uzupełnienie środków */}
        <section className="deposit-section">
          <h2 className="section-title">Uzupełnienie środków na rachunku rozliczeniowym</h2>
          <div className="deposit-form">
            <input
              type="number"
              min="0"
              step="0.01"
              placeholder="Kwota wpłaty (PLN)"
              value={depositAmount}
              onChange={e => setDepositAmount(e.target.value)}
              className="deposit-input"
            />
            <button
              className="btn btn--primary"
              onClick={handleDeposit}
              disabled={depositLoading || !depositAmount}
            >
              {depositLoading ? 'Przetwarzanie…' : 'Wpłać środki'}
            </button>
            {account?.minDepositToRestore > 0 && (
              <span className="deposit-hint">
                Min. wymagana: <strong>{fmt(account.minDepositToRestore)}</strong>
              </span>
            )}
          </div>
        </section>

        {/* Lista przelewów z filtrem */}
        <section className="payments-section">
          <div className="payments-section__header">
            <h2 className="section-title" style={{marginBottom:0}}>Historia przelewów</h2>
            <div className="period-tabs">
              {PERIODS.map(p => (
                <button
                  key={p.days}
                  className={`period-tab ${period === p.days ? 'period-tab--active' : ''}`}
                  onClick={() => setPeriod(p.days)}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>
          <PaymentsList payments={payments} bankId={bankId} />
        </section>
      </main>

      {/* Alert popup — tylko przy przekroczeniu limitu */}
      {alert && <AlertPopup alert={alert} onDismiss={() => setAlert(null)} />}
    </div>
  );
}

function fmt(val) {
  if (val == null) return '—';
  return Number(val).toLocaleString('pl-PL', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' PLN';
}
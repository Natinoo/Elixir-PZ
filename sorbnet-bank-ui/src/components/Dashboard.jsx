import { useEffect, useState, useCallback } from 'react';
import {
  connectWebSocket,
  fetchPayments,
  fetchAccountStatus,
  simulateDeposit,
  createPayment,
  fetchBanks,
} from '../api/sorbnetApi';
import PaymentsList from './PaymentsList';
import AlertPopup from './AlertPopup';
import OverlimitCountdown from './OverlimitCountdown';

const PERIODS = [
  { label: 'Dziś', days: 1 },
  { label: '7 dni', days: 7 },
  { label: '30 dni', days: 30 },
];

export default function Dashboard({ bankId }) {
  const [account, setAccount] = useState(null);
  const [payments, setPayments] = useState([]);
  const [banks, setBanks] = useState([]);
  const [alert, setAlert] = useState(null);
  const [period, setPeriod] = useState(1);
  const [depositAmount, setDepositAmount] = useState('');
  const [depositSource, setDepositSource] = useState('NBP');
  const [depositLoading, setDepositLoading] = useState(false);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [connected, setConnected] = useState(false);
  const [gridlockActive, setGridlockActive] = useState(false);

  const [paymentForm, setPaymentForm] = useState({
    receiverBankId: '',
    senderAccount: '',
    receiverAccount: '',
    amount: '',
    title: '',
  });

  const refreshAccount = useCallback(() => {
    return fetchAccountStatus(bankId).then(setAccount).catch(console.error);
  }, [bankId]);

  const refreshPayments = useCallback((days) => {
    return fetchPayments(bankId, days).then(setPayments).catch(console.error);
  }, [bankId]);

  useEffect(() => {
    fetchBanks().then(setBanks).catch(console.error);
  }, []);

  useEffect(() => {
    refreshAccount();
  }, [refreshAccount]);

  useEffect(() => {
    refreshPayments(period);
  }, [refreshPayments, period]);

  useEffect(() => {
    const client = connectWebSocket({
      bankId,
      onPayment: (payment) => {
        setPayments((prev) => {
          const exists = prev.some((p) => p.paymentId === payment.paymentId);
          return exists
            ? prev.map((p) => (p.paymentId === payment.paymentId ? payment : p))
            : [payment, ...prev];
        });

        refreshAccount();

        if (payment.status === 'SETTLED') {
          setGridlockActive(false);
        }
      },
      onAlert: (alertData) => {
        if (alertData.type === 'DEBT_LIMIT_EXCEEDED') {
          setAlert(alertData);
          setGridlockActive(true);
        } else if (alertData.type === 'APPROACHING_DEBT_LIMIT') {
          setAlert(alertData);
        } else if (alertData.alert === false) {
          setAlert(null);
          setGridlockActive(false);
        }

        refreshAccount();
      },
      onConnect: () => setConnected(true),
      onDisconnect: () => setConnected(false),
    });

    return () => client.deactivate();
  }, [bankId, refreshAccount]);

  const handleDeposit = async () => {
    if (!depositAmount || isNaN(depositAmount)) return;

    setDepositLoading(true);
    try {
      await simulateDeposit(bankId, parseFloat(depositAmount), depositSource);
      setDepositAmount('');
      await refreshAccount();
      await refreshPayments(period);
    } catch (e) {
      console.error(e);
    } finally {
      setDepositLoading(false);
    }
  };

  const handlePaymentChange = (field, value) => {
    setPaymentForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleCreatePayment = async () => {
     console.log('handleCreatePayment wywołany'); 
    console.log('dane formularza:', paymentForm);
    const { receiverBankId, senderAccount, receiverAccount, amount, title } = paymentForm;

    if (!receiverBankId || !senderAccount || !receiverAccount || !amount) return;

    setPaymentLoading(true);
    try {
      await createPayment({
        paymentId: `SORB-${Date.now()}`,
        amount: Number(amount),
        currency: 'PLN',
        senderBankId: bankId,
        receiverBankId: receiverBankId.trim().toUpperCase(),
        senderAccount: senderAccount.trim(),
        receiverAccount: receiverAccount.trim(),
        title: title.trim(),
        status: 'NEW',
      });

      setPaymentForm({
        receiverBankId: '',
        senderAccount: '',
        receiverAccount: '',
        amount: '',
        title: '',
      });

      await refreshAccount();
      await refreshPayments(period);
    } catch (e) {
      console.error(e);
    } finally {
      setPaymentLoading(false);
    }
  };

  const debtUsedPct =
    account?.balance != null && account?.debtLimit
      ? Math.min(100, Math.max(0, (-account.balance / account.debtLimit) * 100))
      : 0;

  const isOverlimit =
    account?.balance != null && account?.debtLimit != null
      ? account.balance < -account.debtLimit
      : false;

  const isApproaching =
    account?.balance != null && account?.debtLimit != null
      ? !isOverlimit && account.balance < -(account.debtLimit * 0.8)
      : false;

  const availableTargetBanks = banks.filter((b) => b.bankId !== bankId);
  const availableDepositSources = banks.filter((b) => b.bankId !== bankId);

  return (
    <div className="dashboard">
      <header className="dash-header">
        <div className="dash-header__left">
          <svg className="dash-logo" viewBox="0 0 32 32" fill="none" aria-label="SORBNET">
            <rect x="2" y="14" width="28" height="4" fill="currentColor" opacity="0.9" />
            <rect x="6" y="8" width="4" height="16" fill="currentColor" />
            <rect x="14" y="8" width="4" height="16" fill="currentColor" />
            <rect x="22" y="8" width="4" height="16" fill="currentColor" />
            <rect x="2" y="22" width="28" height="2" fill="currentColor" opacity="0.5" />
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
        <section className="kpi-row">
          <div className={`kpi-card ${isOverlimit ? 'kpi-card--danger' : isApproaching ? 'kpi-card--warn' : ''}`}>
            <span className="kpi-card__label">Saldo rozrachunkowe</span>
            <span className="kpi-card__value">{account ? fmt(account.balance) : '—'}</span>

            <div className="debt-bar">
              <div
                className={`debt-bar__fill ${
                  isOverlimit ? 'debt-bar__fill--danger' : isApproaching ? 'debt-bar__fill--warn' : ''
                }`}
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
            <span className="kpi-card__label">Status banku</span>
            <span className="kpi-card__value">{account?.blocked ? 'Zablokowany' : 'Aktywny'}</span>
            <span className="kpi-card__sub">
              {account?.blocked ? 'Brak możliwości realizacji przelewów' : 'Bank może wykonywać przelewy'}
            </span>
          </div>

          <div className="kpi-card">
            <span className="kpi-card__label">Przekroczenie limitu</span>
            <span className="kpi-card__value">{isOverlimit ? 'Tak' : 'Nie'}</span>
            <span className="kpi-card__sub">
              {account?.overlimitSince ? `Od: ${new Date(account.overlimitSince).toLocaleString('pl-PL')}` : 'Brak przekroczenia'}
            </span>
          </div>

          <div className="kpi-card">
            <span className="kpi-card__label">
              Transakcje ({PERIODS.find((p) => p.days === period)?.label})
            </span>
            <span className="kpi-card__value">{payments.length}</span>
            <span className="kpi-card__sub">
              Rozliczone: {payments.filter((p) => p.status === 'SETTLED').length}
              {' / '}
              Wstrzymane: {payments.filter((p) => p.status === 'GRIDLOCK_HELD').length}
              {' / '}
              Odrzucone: {payments.filter((p) => p.status === 'REJECTED').length}
            </span>
          </div>
        </section>

        {gridlockActive && (
          <div className="gridlock-banner">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            <span>
              Uruchomiono <strong>gridlock resolution</strong> — system próbuje automatycznie rozliczyć
              wstrzymane przelewy.
            </span>
          </div>
        )}

        {account?.overlimitSince && !account?.blocked && (
          <OverlimitCountdown overlimitSince={account.overlimitSince} />
        )}

        <section className="deposit-section">
          <h2 className="section-title">Uzupełnienie środków na rachunku rozliczeniowym</h2>
 <div className="deposit-form">
    <input
      type="number"
      min="0.01"
      step="0.01"
      placeholder="Kwota wpłaty (PLN)"
      value={depositAmount}
      onChange={(e) => setDepositAmount(e.target.value)}
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

        <section className="deposit-section">
          <h2 className="section-title">Nowy przelew SORBNET</h2>

          <div className="deposit-form" style={{ flexWrap: 'wrap' }}>
            <select
              className="deposit-input"
              value={paymentForm.receiverBankId}
              onChange={(e) => handlePaymentChange('receiverBankId', e.target.value)}
            >
              <option value="">Wybierz bank odbiorcy</option>
              {availableTargetBanks.map((b) => (
                <option key={b.bankId} value={b.bankId}>
                  {b.bankId} — {b.bankName}
                </option>
              ))}
            </select>

            <input
              className="deposit-input"
              placeholder="Rachunek nadawcy"
              value={paymentForm.senderAccount}
              onChange={(e) => handlePaymentChange('senderAccount', e.target.value)}
            />

            <input
              className="deposit-input"
              placeholder="Rachunek odbiorcy"
              value={paymentForm.receiverAccount}
              onChange={(e) => handlePaymentChange('receiverAccount', e.target.value)}
            />

            <input
              className="deposit-input"
              type="number"
              min="0"
              step="0.01"
              placeholder="Kwota"
              value={paymentForm.amount}
              onChange={(e) => handlePaymentChange('amount', e.target.value)}
            />

            <input
              className="deposit-input"
              placeholder="Tytuł przelewu"
              value={paymentForm.title}
              onChange={(e) => handlePaymentChange('title', e.target.value)}
            />

            <button
              className="btn btn--primary"
              onClick={handleCreatePayment}
              disabled={
                paymentLoading ||
                !paymentForm.receiverBankId ||
                !paymentForm.senderAccount ||
                !paymentForm.receiverAccount ||
                !paymentForm.amount
              }
            >
              {paymentLoading ? 'Wysyłanie…' : 'Wyślij przelew'}
            </button>
          </div>
        </section>

        <section className="payments-section">
          <div className="payments-section__header">
            <h2 className="section-title" style={{ marginBottom: 0 }}>
              Historia przelewów
            </h2>

            <div className="period-tabs">
              {PERIODS.map((p) => (
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

      {alert && <AlertPopup alert={alert} onDismiss={() => setAlert(null)} />}
    </div>
  );
}

function fmt(val) {
  if (val == null) return '—';
  return (
    Number(val).toLocaleString('pl-PL', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' PLN'
  );
}
import { useState, useEffect } from 'react';
import { fetchBankInfo, fetchPayments, sendPayment } from '../api/sorbnetApi';
import { useWebSocket } from '../hooks/useWebSocket';
import AlertPopup from './AlertPopup';
import OverlimitCountdown from './OverlimitCountdown';
const BANK_ID = 'PKO'; // ← zmień na swój bank

export default function Dashboard() {
  const bankId = BANK_ID;
  const [bank, setBank] = useState(null);
  const [payments, setPayments] = useState([]);
  const [loadingBank, setLoadingBank] = useState(true);
  const [loadingPayments, setLoadingPayments] = useState(true);
  const [alert, setAlert] = useState(null);
  const [daysBack, setDaysBack] = useState(1);
  const [topupAmount, setTopupAmount] = useState('');
  const [topupSender, setTopupSender] = useState('NBP');
  const [topupLoading, setTopupLoading] = useState(false);
  const [topupMsg, setTopupMsg] = useState(null);

  const loadBank = () =>
    fetchBankInfo(bankId)
      .then(data => setBank(data || { bankId, bankName: bankId, balance: 0, debtLimit: 0, blocked: false }))
      .catch(() => setBank({ bankId, bankName: bankId, balance: 0, debtLimit: 0, blocked: false }))
      .finally(() => setLoadingBank(false));

  const loadPayments = () => {
  setLoadingPayments(true);
  fetchPayments(bankId, daysBack)  
    .then(setPayments)
    .catch(() => setPayments([]))
    .finally(() => setLoadingPayments(false));
};

  useEffect(() => { loadBank(); }, []);
  useEffect(() => { loadPayments(); }, [daysBack]);

  useWebSocket([`/topic/alerts/${bankId}`], (topic, msg) => {
    setAlert(msg);
    loadBank();
    loadPayments();
  });

  const handleTopup = async () => {
    if (!topupAmount || isNaN(topupAmount) || parseFloat(topupAmount) <= 0) return;
    setTopupLoading(true);
    setTopupMsg(null);
    try {
      await sendPayment({
        senderBankId: topupSender,
        receiverBankId: bankId,
        amount: parseFloat(topupAmount),
        title: 'Dokapitalizowanie rachunku rozliczeniowego'
      });
      setTopupMsg({ type: 'success', text: `Przelew ${fmt(parseFloat(topupAmount))} z ${topupSender} wysłany pomyślnie.` });
      setTopupAmount('');
      setTimeout(() => { loadBank(); loadPayments(); }, 1000);
    } catch {
      setTopupMsg({ type: 'error', text: 'Błąd wysyłania przelewu. Sprawdź czy bank nadawcy jest aktywny.' });
    } finally {
      setTopupLoading(false);
    }
  };

  const fmt = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n ?? 0);
  const fmtDate = (d) => d ? new Date(d).toLocaleString('pl-PL') : '—';

  const isOverlimit = bank && bank.balance < -(bank.debtLimit ?? 0);
  const shortfall = bank ? Math.max(0, -bank.balance - (bank.debtLimit ?? 0)) : 0;

  const STATUS_LABELS = {
    SETTLED: { label: 'Rozliczony', cls: 'badge-success' },
    PENDING: { label: 'Oczekujący', cls: 'badge-neutral' },
    GRIDLOCK_HELD: { label: 'Gridlock', cls: 'badge-warning' },
    REJECTED: { label: 'Odrzucony', cls: 'badge-error' },
  };

  if (loadingBank) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100dvh', color: 'var(--color-text-muted)' }}>
      Ładowanie...
    </div>
  );

  if (!bank && !loadingBank) return (
  <p style={{ padding: '2rem' }}>Nie znaleziono banku: {bankId}</p>
  );

  return (
    <div className="layout">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo">SORBNET</div>
        <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', padding: '0 var(--space-2)' }}>Zalogowany jako</div>
        <div style={{ fontWeight: 700, marginBottom: 'var(--space-6)', padding: '0 var(--space-2)', fontSize: '1.1rem' }}>{bankId}</div>
        <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', padding: '0 var(--space-2)', marginBottom: 'var(--space-1)' }}>Saldo</div>
        <div style={{ fontWeight: 700, fontVariantNumeric: 'tabular-nums', padding: '0 var(--space-2)', marginBottom: 'var(--space-4)', color: bank.balance < 0 ? 'var(--color-error)' : 'var(--color-success)', fontSize: '0.95rem' }}>
          {fmt(bank.balance)}
        </div>
        <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', padding: '0 var(--space-2)', marginBottom: 'var(--space-1)' }}>Limit zadłużenia</div>
        <div style={{ fontVariantNumeric: 'tabular-nums', padding: '0 var(--space-2)', marginBottom: 'var(--space-6)', fontSize: '0.9rem' }}>
          {fmt(bank.debtLimit)}
        </div>
        <div style={{ padding: '0 var(--space-2)' }}>
          <span className={`badge ${bank.blocked ? 'badge-error' : isOverlimit ? 'badge-warning' : 'badge-success'}`}>
            {bank.blocked ? 'ZABLOKOWANY' : isOverlimit ? 'PONAD LIMIT' : 'AKTYWNY'}
          </span>
        </div>
      </aside>

      {/* Main */}
      <main className="main-content">

        {bank.overlimitSince && !bank.blocked && (
        <div style={{ marginBottom: 'var(--space-6)' }}>
          <OverlimitCountdown overlimitSince={bank.overlimitSince} />
        </div>
        )}

        {/* Alert przy przekroczeniu limitu */}
        {(isOverlimit || bank.blocked) && (
          <div className="card" style={{
            background: bank.blocked ? 'var(--color-error-highlight)' : 'var(--color-warning-highlight)',
            borderColor: bank.blocked ? 'var(--color-error)' : 'var(--color-warning)',
            marginBottom: 'var(--space-6)'
          }}>
            <div style={{ fontWeight: 700, marginBottom: 'var(--space-2)', color: bank.blocked ? 'var(--color-error)' : 'var(--color-warning)' }}>
              {bank.blocked ? '🔴 Bank jest zablokowany' : '⚠ Przekroczono limit zadłużenia'}
            </div>
            {isOverlimit && !bank.blocked && (
              <p style={{ fontSize: '0.875rem', marginBottom: 'var(--space-1)' }}>
                Brakuje <strong>{fmt(shortfall)}</strong> do odzyskania płynności.
                Minimalna kwota dokapitalizowania: <strong>{fmt(shortfall + 1)}</strong>
              </p>
            )}
            {bank.overlimitSince && (
              <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                Ponad limit od: {fmtDate(bank.overlimitSince)} — automatyczna blokada po 2h
              </p>
            )}
          </div>
        )}

        {/* Formularz dokapitalizowania */}
        <div className="card" style={{ marginBottom: 'var(--space-6)' }}>
          <div style={{ fontWeight: 600, marginBottom: 'var(--space-4)' }}>Symulacja dokapitalizowania</div>
          <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--color-text-muted)', marginBottom: 'var(--space-1)' }}>Bank nadawcy</label>
              <select
                value={topupSender}
                onChange={e => setTopupSender(e.target.value)}
                style={{ padding: 'var(--space-2) var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', background: 'var(--color-surface)', font: 'inherit', fontSize: '0.875rem' }}
              >
                <option value="NBP">NBP (bank centralny)</option>
                <option value="ING">ING</option>
                <option value="PEKAO">PEKAO</option>
                <option value="MBANK">MBANK</option>
                <option value="BNP">BNP</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.75rem', color: 'var(--color-text-muted)', marginBottom: 'var(--space-1)' }}>Kwota (PLN)</label>
              <input
                type="number"
                placeholder="np. 1000000"
                value={topupAmount}
                onChange={e => setTopupAmount(e.target.value)}
                style={{ padding: 'var(--space-2) var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', font: 'inherit', fontSize: '0.875rem', width: '180px' }}
              />
            </div>
            {isOverlimit && shortfall > 0 && (
              <button
                className="btn btn-ghost"
                style={{ fontSize: '0.8rem' }}
                onClick={() => setTopupAmount(String(Math.ceil(shortfall + 1)))}
              >
                Wstaw minimum ({fmt(shortfall + 1)})
              </button>
            )}
            <button
              className="btn btn-primary"
              onClick={handleTopup}
              disabled={topupLoading || !topupAmount}
            >
              {topupLoading ? 'Wysyłanie...' : 'Wyślij przelew'}
            </button>
          </div>
          {topupMsg && (
            <div style={{ marginTop: 'var(--space-3)', fontSize: '0.875rem', color: topupMsg.type === 'success' ? 'var(--color-success)' : 'var(--color-error)' }}>
              {topupMsg.text}
            </div>
          )}
        </div>

        {/* Historia przelewów */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 'var(--space-4)' }}>
          <h2 style={{ fontSize: '1rem', fontWeight: 600 }}>Historia przelewów</h2>
          <select
            value={daysBack}
            onChange={e => setDaysBack(Number(e.target.value))}
            style={{ padding: 'var(--space-2) var(--space-3)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', background: 'var(--color-surface)', font: 'inherit', fontSize: '0.875rem' }}
          >
            <option value={1}>Dzisiaj</option>
            <option value={30}>Ostatnie 30 dni</option>
          </select>
        </div>

        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {loadingPayments ? (
            <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie przelewów...</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Kierunek</th>
                  <th>Kontrahent</th>
                  <th>Kwota</th>
                  <th>Tytuł</th>
                  <th>Status</th>
                  <th>Data</th>
                </tr>
              </thead>
              <tbody>
                {payments.map(p => {
                  const isOut = p.senderBankId === bankId;
                  const s = STATUS_LABELS[p.status] || { label: p.status, cls: 'badge-neutral' };
                  return (
                    <tr key={p.paymentId}>
                      <td>
                        <span className={`badge ${isOut ? 'badge-error' : 'badge-success'}`}>
                          {isOut ? '↑ Wychodzący' : '↓ Przychodzący'}
                        </span>
                      </td>
                      <td>{isOut ? p.receiverBankId : p.senderBankId}</td>
                      <td style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}>
                        <span style={{ color: isOut ? 'var(--color-error)' : 'var(--color-success)' }}>
                          {isOut ? '−' : '+'}{fmt(p.amount)}
                        </span>
                      </td>
                      <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--color-text-muted)', fontSize: '0.875rem' }}>
                        {p.title}
                      </td>
                      <td><span className={`badge ${s.cls}`}>{s.label}</span></td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                        {fmtDate(p.settledAt || p.createdAt)}
                      </td>
                    </tr>
                  );
                })}
                {payments.length === 0 && (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--color-text-muted)' }}>
                      Brak przelewów w wybranym okresie
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </main>

      <AlertPopup alert={alert} onClose={() => setAlert(null)} />
    </div>
  );
}
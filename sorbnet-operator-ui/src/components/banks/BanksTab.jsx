import { useState, useEffect } from 'react';
import { fetchBanks, blockBank, unblockBank } from '../../api/sorbnetApi';

export default function BanksTab() {
  const [banks, setBanks] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = () => fetchBanks().then(setBanks).finally(() => setLoading(false));

  useEffect(() => { load(); }, []);

  const handleBlock = async (bankId) => {
    await blockBank(bankId);
    load();
  };

  const handleUnblock = async (bankId) => {
    await unblockBank(bankId);
    load();
  };

  const fmt = (n) => new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' }).format(n);

  return (
    <div>
      <h1 className="page-title">Rachunki rozliczeniowe banków</h1>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {loading ? <p style={{ padding: '2rem', color: 'var(--color-text-muted)' }}>Ładowanie...</p> : (
          <table>
            <thead>
              <tr>
                <th>Bank</th>
                <th>Saldo</th>
                <th>Limit zadłużenia</th>
                <th>Status</th>
                <th>Akcja</th>
              </tr>
            </thead>
            <tbody>
              {banks.map(b => (
                <tr key={b.bankId}>
                  <td><strong>{b.bankId}</strong><br /><span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>{b.bankName}</span></td>
                  <td style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(b.balance)}</td>
                  <td style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(b.debtLimit)}</td>
                  <td>
                    {b.blocked
                      ? <span className="badge badge-error">ZABLOKOWANY</span>
                      : b.overlimitSince
                        ? <span className="badge badge-warning">PONAD LIMIT</span>
                        : <span className="badge badge-success">AKTYWNY</span>
                    }
                  </td>
                  <td>
                    {b.blocked
                      ? <button className="btn btn-success" onClick={() => handleUnblock(b.bankId)}>Odblokuj</button>
                      : <button className="btn btn-danger" onClick={() => handleBlock(b.bankId)}>Zablokuj</button>
                    }
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
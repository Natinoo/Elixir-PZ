import { useState } from 'react';
import BanksTab from './components/banks/BanksTab';
import PaymentsTab from './components/payments/PaymentsTab';
import GridlockTab from './components/gridlock/GridlockTab';
import EmergenciesTab from './components/emergencies/EmergenciesTab';

const TABS = [
  { id: 'banks', label: 'Banki', icon: '🏦' },
  { id: 'payments', label: 'Transakcje', icon: '↔' },
  { id: 'gridlock', label: 'Gridlock', icon: '⏳' },
  { id: 'emergencies', label: 'Emergencies', icon: '⚠', alert: true },
];

export default function App() {
  const [activeTab, setActiveTab] = useState('banks');
  const [emergencyCount, setEmergencyCount] = useState(0);

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">SORBNET NBP</div>
        {TABS.map(tab => (
          <button
            key={tab.id}
            className={`nav-item ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            <span>{tab.icon}</span>
            {tab.label}
            {tab.alert && emergencyCount > 0 && <span className="badge-dot" />}
          </button>
        ))}
      </aside>
      <main className="main-content">
        {activeTab === 'banks' && <BanksTab />}
        {activeTab === 'payments' && <PaymentsTab />}
        {activeTab === 'gridlock' && <GridlockTab />}
        {activeTab === 'emergencies' && <EmergenciesTab onCountChange={setEmergencyCount} />}
      </main>
    </div>
  );
}
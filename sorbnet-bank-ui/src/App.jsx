import Dashboard from './components/Dashboard';

const params = new URLSearchParams(window.location.search);
const bankId = params.get('bank') || 'BANK_A';

export default function App() {
  return <Dashboard bankId={bankId} />;
}
import Dashboard from './components/Dashboard';

const BANK_ID = 'PKO'; // kazdy zespoł zmienia na swoj bank

export default function App() {
  return <Dashboard bankId={BANK_ID} onLogout={() => {}} />;
}
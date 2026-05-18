import axios from 'axios';

const BASE = 'http://localhost:8083/api/sorbnet/operator';

export const fetchBanks = () => axios.get(`${BASE}/banks`).then(r => r.data);
export const fetchPayments = (params) => axios.get(`${BASE}/payments`, { params }).then(r => r.data);
export const fetchGridlock = () => axios.get(`${BASE}/gridlock`).then(r => r.data);
export const fetchEmergencies = () => axios.get(`${BASE}/emergencies`).then(r => r.data);
export const blockBank = (bankId) => axios.post(`${BASE}/banks/${bankId}/block`).then(r => r.data);
export const unblockBank = (bankId) => axios.post(`${BASE}/banks/${bankId}/unblock`).then(r => r.data);
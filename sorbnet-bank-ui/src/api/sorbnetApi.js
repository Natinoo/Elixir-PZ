import axios from 'axios';

const BASE = 'http://localhost:8083/api/sorbnet';

export const fetchBankInfo = (bankId) =>
  axios.get(`${BASE}/operator/banks`).then(r => r.data.find(b => b.bankId === bankId));

export const fetchPayments = (bankId, daysBack = 1) => {
  const from = new Date();
  from.setDate(from.getDate() - (daysBack - 1));
  from.setHours(0, 0, 0, 0);
  const fromStr = from.toISOString().slice(0, 10);

  return axios
    .get(`${BASE}/payments`, { params: { bankId, from: fromStr } })
    .then(r => r.data);
};

export const sendPayment = (data) =>
  axios.post(`${BASE}/payments`, data).then(r => r.data);
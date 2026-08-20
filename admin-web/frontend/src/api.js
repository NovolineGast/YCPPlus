import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const auth = {
  init: (appName, password) =>
    api.post('/init', { appName, password }),

  login: (appName, password) =>
    api.post('/login', { appName, password }),
};

export const dashboard = {
  getStats: () => api.get('/dashboard/stats'),
};

export const keys = {
  getAll: () => api.get('/keys'),

  generate: (amount, days, prefix) =>
    api.post('/keys/generate', { amount, days, prefix }),

  ban: (key) => api.post(`/keys/${key}/ban`),

  unban: (key) => api.post(`/keys/${key}/unban`),

  delete: (key) => api.delete(`/keys/${key}`),

  getFingerprint: (key) => api.get(`/keys/${key}/fingerprint`),
};

export default api;

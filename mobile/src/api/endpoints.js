import client from './client';

export const authApi = {
  login: (email, password) => client.post('/api/auth/login', { email, password }),
};

export const productsApi = {
  // Résolution QR : le contenu du QR est la RÉFÉRENCE produit.
  byQr: (code) => client.get('/api/products/by-qr', { params: { code } }),
  search: (params) => client.get('/api/products', { params }),
  get: (id) => client.get(`/api/products/${id}`),
  lowStock: () => client.get('/api/products/low-stock'),
};

export const salesApi = {
  create: (payload) => client.post('/api/sales', payload),
  mine: (params) => client.get('/api/sales/mine', { params }),
  detail: (id) => client.get(`/api/sales/${id}`),
};

export const returnsApi = {
  create: (payload) => client.post('/api/returns', payload),
};

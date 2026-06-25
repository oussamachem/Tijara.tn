import client from './client';

export const authApi = {
  login: (email, password) => client.post('/api/auth/login', { email, password }),
};

export const productsApi = {
  // Liste des produits (chaque produit porte ses variantes).
  search: (params) => client.get('/api/products', { params }),
  get: (id) => client.get(`/api/products/${id}`),
};

export const variantsApi = {
  // Résolution scan : le contenu du QR est la RÉFÉRENCE de la VARIANTE.
  byQr: (code) => client.get('/api/variants/by-qr', { params: { code } }),
  lowStock: () => client.get('/api/variants/low-stock'),
};

export const salesApi = {
  create: (payload) => client.post('/api/sales', payload),
  mine: (params) => client.get('/api/sales/mine', { params }),
  detail: (id) => client.get(`/api/sales/${id}`),
};

export const returnsApi = {
  create: (payload) => client.post('/api/returns', payload),
};

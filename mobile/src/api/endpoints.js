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

export const reservationsApi = {
  // Acompte / Reservation (layaway). Le total est calcule serveur.
  create: (payload) => client.post('/api/reservations', payload),
  list: (status) => client.get('/api/reservations', { params: status ? { status } : {} }),
  dueSoon: () => client.get('/api/reservations/due-soon'),
  detail: (id) => client.get(`/api/reservations/${id}`),
  pay: (id, payload) => client.post(`/api/reservations/${id}/payments`, payload),
  cancel: (id) => client.post(`/api/reservations/${id}/cancel`),
};

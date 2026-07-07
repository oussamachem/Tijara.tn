import client from './client';

export const authApi = {
  login: (email, password) => client.post('/api/auth/login', { email, password }),
  register: (fullName, email, password) => client.post('/api/auth/register', { fullName, email, password }),
};

export const shopsApi = {
  search: (query) => client.get('/api/shops', { params: query ? { query } : {} }),
  catalog: (slug) => client.get(`/api/shops/${slug}/products`),
  order: (slug, items) => client.post(`/api/shops/${slug}/orders`, { items }),
  myOrders: (slug) => client.get(`/api/shops/${slug}/orders/mine`),
};

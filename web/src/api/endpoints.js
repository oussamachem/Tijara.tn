import client from './client.js';

// ===================================== IDENTITÉ / AUTH ==========================================
export const authApi = {
  login: (email, password) => client.post('/api/auth/login', { email, password }),
  register: (fullName, email, password) => client.post('/api/auth/register', { fullName, email, password }),
  forgotPassword: (email) => client.post('/api/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => client.post('/api/auth/reset-password', { token, newPassword }),
};

// Boutiques dont l'utilisateur connecté est membre (aiguillage OWNER/VENDOR + sélecteur). Identité,
// donc SANS X-Shop-Id.
export const meApi = {
  shops: () => client.get('/api/me/shops'),
};

export const profileApi = {
  get: () => client.get('/api/profile'),
  update: (payload) => client.put('/api/profile', payload),
  changePassword: (payload) => client.put('/api/profile/password', payload),
};

// ================================= MARKETPLACE (CLIENT, PUBLIC) =================================
// Tenant résolu par le SLUG. AUCUN X-Shop-Id (routes /api/shops/**).
export const marketplaceApi = {
  search: (query) => client.get('/api/shops', { params: query ? { query } : {} }),
  catalog: (slug) => client.get(`/api/shops/${slug}/products`),
  gallery: (slug, page = 0, size = 24) => client.get(`/api/shops/${slug}/gallery`, { params: { page, size } }),
  order: (slug, items) => client.post(`/api/shops/${slug}/orders`, { items }),
  myOrders: (slug) => client.get(`/api/shops/${slug}/orders/mine`),
};

// ============================= PLATEFORME (PLATFORM_ADMIN, super-admin) =========================
export const boutiquesApi = {
  list: () => client.get('/api/admin/boutiques'),
  create: (payload) => client.post('/api/admin/boutiques', payload),
  suspend: (id) => client.post(`/api/admin/boutiques/${id}/suspend`),
  reactivate: (id) => client.post(`/api/admin/boutiques/${id}/reactivate`),
};

// ================================ CAISSE / VENDEUR (boutique active) ============================
// Toutes ces routes sont SCOPÉES par la boutique active -> X-Shop-Id posé par l'interceptor.
export const posApi = {
  byQr: (code) => client.get('/api/variants/by-qr', { params: { code } }),
  productSearch: (params) => client.get('/api/products', { params }),
  createSale: (payload) => client.post('/api/sales', payload),
  mySales: (params) => client.get('/api/sales/mine', { params }),
  saleDetail: (id) => client.get(`/api/sales/${id}`),
  createReturn: (payload) => client.post('/api/returns', payload),
};

export const reservationsApi = {
  list: (status) => client.get('/api/reservations', { params: status ? { status } : {} }),
  dueSoon: () => client.get('/api/reservations/due-soon'),
  detail: (id) => client.get(`/api/reservations/${id}`),
  create: (payload) => client.post('/api/reservations', payload),
  pay: (id, payload) => client.post(`/api/reservations/${id}/payments`, payload),
  cancel: (id) => client.post(`/api/reservations/${id}/cancel`),
};

// ===================================== ADMIN (OWNER) ============================================
export const dashboardApi = {
  get: () => client.get('/api/dashboard'),
};

export const categoriesApi = {
  list: () => client.get('/api/categories'),
  create: (payload) => client.post('/api/admin/categories', payload),
  update: (id, payload) => client.put(`/api/admin/categories/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/categories/${id}`),
};

export const colorsApi = {
  list: () => client.get('/api/colors'),
  create: (payload) => client.post('/api/admin/colors', payload),
  update: (id, payload) => client.put(`/api/admin/colors/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/colors/${id}`),
};

export const sizesApi = {
  list: () => client.get('/api/sizes'),
  create: (payload) => client.post('/api/admin/sizes', payload),
  update: (id, payload) => client.put(`/api/admin/sizes/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/sizes/${id}`),
};

export const productsApi = {
  search: (params) => client.get('/api/products', { params }),
  get: (id) => client.get(`/api/products/${id}`),
  create: (payload) => client.post('/api/admin/products', payload),
  updateHeader: (id, payload) => client.put(`/api/admin/products/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/products/${id}`),
  addVariant: (id, payload) => client.post(`/api/admin/products/${id}/variants`, payload),
  uploadImages: (id, files) => {
    const form = new FormData();
    Array.from(files).forEach((f) => form.append('files', f));
    return client.post(`/api/admin/products/${id}/images`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  deleteImage: (id, imageId) => client.delete(`/api/admin/products/${id}/images/${imageId}`),
  reorderImages: (id, imageIds) => client.put(`/api/admin/products/${id}/images/order`, { imageIds }),
};

export const variantsApi = {
  byQr: (code) => client.get('/api/variants/by-qr', { params: { code } }),
  lowStock: () => client.get('/api/variants/low-stock'),
  remove: (id) => client.delete(`/api/admin/variants/${id}`),
  setStock: (id, quantity) => client.patch(`/api/admin/variants/${id}/stock`, { quantity }),
  adjustStock: (id, delta) => client.patch(`/api/admin/variants/${id}/stock/adjust`, { delta }),
  qrCode: (id) => client.get(`/api/variants/${id}/qrcode`, { responseType: 'blob' }),
};

export const sellersApi = {
  list: () => client.get('/api/admin/sellers'),
  create: (payload) => client.post('/api/admin/sellers', payload),
  update: (id, payload) => client.put(`/api/admin/sellers/${id}`, payload),
  activate: (id) => client.patch(`/api/admin/sellers/${id}/activate`),
  deactivate: (id) => client.patch(`/api/admin/sellers/${id}/deactivate`),
};

export const customersApi = {
  list: () => client.get('/api/admin/customers'),
  create: (payload) => client.post('/api/admin/customers', payload),
  update: (id, payload) => client.put(`/api/admin/customers/${id}`, payload),
};

export const creditsApi = {
  list: (params) => client.get('/api/admin/credits', { params }),
  get: (id) => client.get(`/api/admin/credits/${id}`),
  create: (payload) => client.post('/api/admin/credits', payload),
  pay: (id, payload) => client.post(`/api/admin/credits/${id}/payments`, payload),
  cancel: (id) => client.post(`/api/admin/credits/${id}/cancel`),
  dashboard: () => client.get('/api/admin/credits/dashboard'),
};

export const suppliersApi = {
  list: () => client.get('/api/admin/suppliers'),
  create: (payload) => client.post('/api/admin/suppliers', payload),
  update: (id, payload) => client.put(`/api/admin/suppliers/${id}`, payload),
};

export const debtsApi = {
  list: (params) => client.get('/api/admin/debts', { params }),
  get: (id) => client.get(`/api/admin/debts/${id}`),
  create: (payload) => client.post('/api/admin/debts', payload),
  pay: (id, payload) => client.post(`/api/admin/debts/${id}/payments`, payload),
  remove: (id) => client.delete(`/api/admin/debts/${id}`),
  dashboard: () => client.get('/api/admin/debts/dashboard'),
};

export const salesApi = {
  history: (params) => client.get('/api/admin/sales', { params }),
  detail: (id) => client.get(`/api/sales/${id}`),
};

export const returnsApi = {
  history: (params) => client.get('/api/admin/returns', { params }),
};

export const ordersApi = {
  list: (params) => client.get('/api/admin/orders', { params }),
  get: (id) => client.get(`/api/admin/orders/${id}`),
  changeStatus: (id, status) => client.post(`/api/admin/orders/${id}/status`, { status }),
};

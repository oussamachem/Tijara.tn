import client from './client.js';

// ----------------------------- Authentification -----------------------------
export const authApi = {
  login: (email, password) => client.post('/api/auth/login', { email, password }),
  forgotPassword: (email) => client.post('/api/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => client.post('/api/auth/reset-password', { token, newPassword }),
};

// --------------------------------- Dashboard ---------------------------------
export const dashboardApi = {
  get: () => client.get('/api/dashboard'),
};

// -------------------------------- Catégories ---------------------------------
export const categoriesApi = {
  list: () => client.get('/api/categories'),
  sizeOptions: (id) => client.get(`/api/categories/${id}/size-options`),
  create: (payload) => client.post('/api/admin/categories', payload),
  update: (id, payload) => client.put(`/api/admin/categories/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/categories/${id}`),
};

// --------------------------------- Couleurs ----------------------------------
export const colorsApi = {
  list: () => client.get('/api/colors'),
  create: (payload) => client.post('/api/admin/colors', payload),
  update: (id, payload) => client.put(`/api/admin/colors/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/colors/${id}`),
};

// --------------------------------- Produits ----------------------------------
export const productsApi = {
  search: (params) => client.get('/api/products', { params }),
  get: (id) => client.get(`/api/products/${id}`),
  // create payload = { reference, name, description, categoryId, purchasePrice, salePrice, variants:[{colorId,size,quantity,seuilAlerte}] }
  create: (payload) => client.post('/api/admin/products', payload),
  updateHeader: (id, payload) => client.put(`/api/admin/products/${id}`, payload),
  remove: (id) => client.delete(`/api/admin/products/${id}`),
  addVariant: (id, payload) => client.post(`/api/admin/products/${id}/variants`, payload),
  uploadImage: (id, file) => {
    const form = new FormData();
    form.append('file', file);
    return client.post(`/api/admin/products/${id}/image`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// --------------------------------- Variantes ---------------------------------
export const variantsApi = {
  byQr: (code) => client.get('/api/variants/by-qr', { params: { code } }),
  lowStock: () => client.get('/api/variants/low-stock'),
  remove: (id) => client.delete(`/api/admin/variants/${id}`),
  setStock: (id, quantity) => client.patch(`/api/admin/variants/${id}/stock`, { quantity }),
  adjustStock: (id, delta) => client.patch(`/api/admin/variants/${id}/stock/adjust`, { delta }),
  // Image PNG du QR Code de la variante (blob, avec auth).
  qrCode: (id) => client.get(`/api/variants/${id}/qrcode`, { responseType: 'blob' }),
};

// --------------------------------- Vendeurs ----------------------------------
export const sellersApi = {
  list: () => client.get('/api/admin/sellers'),
  create: (payload) => client.post('/api/admin/sellers', payload),
  update: (id, payload) => client.put(`/api/admin/sellers/${id}`, payload),
  activate: (id) => client.patch(`/api/admin/sellers/${id}/activate`),
  deactivate: (id) => client.patch(`/api/admin/sellers/${id}/deactivate`),
};

// ------------------------------ Ventes / Retours -----------------------------
export const salesApi = {
  history: (params) => client.get('/api/admin/sales', { params }),
  detail: (id) => client.get(`/api/sales/${id}`),
};

export const returnsApi = {
  history: (params) => client.get('/api/admin/returns', { params }),
};

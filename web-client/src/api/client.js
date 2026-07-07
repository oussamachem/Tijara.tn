import axios from 'axios';

export const TOKEN_KEY = 'sbc_token';
export const USER_KEY = 'sbc_user';

// Build derriere nginx : VITE_API_URL="" -> baseURL relative (meme origine, proxy /api).
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 sur un appel authentifie (hors /auth) = session expiree -> purge + retour connexion.
client.interceptors.response.use(
  (r) => r,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const hadToken = !!localStorage.getItem(TOKEN_KEY);
    if (status === 401 && hadToken && !url.includes('/api/auth/')) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
    return Promise.reject(error);
  }
);

export function apiError(error, fallback = 'Une erreur est survenue') {
  const data = error?.response?.data;
  if (data?.message) return data.message;
  if (Array.isArray(data?.errors) && data.errors.length) return data.errors.join(', ');
  if (error?.message === 'Network Error') return 'Impossible de joindre le serveur.';
  return fallback;
}

export default client;

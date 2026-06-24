import axios from 'axios';
import Constants from 'expo-constants';
import * as SecureStore from 'expo-secure-store';

export const API_BASE_URL =
  Constants.expoConfig?.extra?.apiBaseUrl || 'http://10.0.2.2:8080';

export const TOKEN_KEY = 'sb_token';

const client = axios.create({ baseURL: API_BASE_URL, timeout: 15000 });

// Ajoute le JWT (lu dans le SecureStore) à chaque requête.
client.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handler de session expirée, fourni par l'AuthContext (évite un couplage dur).
let onUnauthorized = null;
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

// 401 = session expirée UNIQUEMENT hors endpoints d'auth (un login raté ne doit pas
// déclencher de déconnexion : il affiche « identifiants invalides »). Même leçon que le web.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const isAuthEndpoint = url.includes('/api/auth/');
    if (status === 401 && !isAuthEndpoint && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(error);
  }
);

/** Message d'erreur lisible (français) à partir d'une erreur Axios. */
export function apiError(error, fallback = 'Une erreur est survenue') {
  const data = error?.response?.data;
  if (data?.message) return data.message;
  if (Array.isArray(data?.errors) && data.errors.length) return data.errors.join(', ');
  if (error?.code === 'ECONNABORTED') return 'Délai dépassé. Réseau lent ou serveur injoignable.';
  if (error?.message === 'Network Error') return 'Impossible de joindre le serveur. Vérifiez la connexion.';
  return fallback;
}

export default client;

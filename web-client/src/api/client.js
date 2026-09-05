import axios from 'axios';
import { keycloak, freshToken } from '../auth/keycloak.js';

// Build derriere nginx : VITE_API_URL="" -> baseURL relative (meme origine, proxy /api).
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
});

// Intercepteur ASYNCHRONE : access token Keycloak FRAIS (rafraîchi s'il expire dans < 30 s).
client.interceptors.request.use(async (config) => {
  const token = await freshToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 sur un appel authentifié = session Keycloak expirée -> retour page de connexion Keycloak.
client.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && keycloak.authenticated) {
      keycloak.clearToken();
      keycloak.login({ redirectUri: window.location.origin + '/' });
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

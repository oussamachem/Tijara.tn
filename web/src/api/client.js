import axios from 'axios';
import { keycloak, freshToken } from '../auth/keycloak.js';

// VITE_API_URL non défini (dev) -> localhost:8080. Défini mais vide (build derrière nginx)
// -> baseURL relative '' : les appels /api et /uploads passent par le même origine (proxy nginx).
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
});

// -------------------------------------------------------------------------------------------------
// Boutique active (X-Shop-Id). Source de vérité posée par le ShopContext.
// L'en-tête n'est ajouté QUE si une boutique est active (mode "boutique" : OWNER/VENDOR) ET que la
// route est SCOPÉE par tenant. On l'EXCLUT des routes MARKETPLACE publiques (/api/shops/**) dont le
// tenant est résolu par le SLUG, pas par X-Shop-Id.
// -------------------------------------------------------------------------------------------------
let activeShopId = null;
export function setActiveShopId(id) {
  activeShopId = id != null ? String(id) : null;
}
export function getActiveShopId() {
  return activeShopId;
}

// Intercepteur ASYNCHRONE : on récupère un access token Keycloak FRAIS (rafraîchi s'il expire dans
// < 30 s) juste avant l'envoi. Plus de token en localStorage : keycloak-js est la source de vérité.
client.interceptors.request.use(async (config) => {
  const token = await freshToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;

  const url = config.url || '';
  const isMarketplaceRoute = url.includes('/api/shops'); // catalogue/galerie/commandes -> tenant par slug
  if (activeShopId != null && !isMarketplaceRoute) {
    config.headers['X-Shop-Id'] = activeShopId;
  }
  return config;
});

// 401 = session Keycloak expirée (le refresh token a lui aussi expiré) alors qu'on était connecté :
// on renvoie l'utilisateur sur la page de connexion Keycloak. Un 403 (X-Shop-Id d'une boutique dont
// on n'est pas membre) N'EST PAS traité ici : c'est une autorisation refusée, pas une session morte.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401 && keycloak.authenticated) {
      keycloak.clearToken();
      keycloak.login({ redirectUri: window.location.origin + '/' });
    }
    return Promise.reject(error);
  }
);

/** Extrait un message d'erreur lisible (français) d'une erreur Axios. */
export function apiError(error, fallback = 'Une erreur est survenue') {
  const data = error?.response?.data;
  if (data?.message) return data.message;
  if (Array.isArray(data?.errors) && data.errors.length) return data.errors.join(', ');
  if (error?.message === 'Network Error') return "Impossible de joindre le serveur. Vérifiez que l'API est démarrée.";
  return fallback;
}

export default client;

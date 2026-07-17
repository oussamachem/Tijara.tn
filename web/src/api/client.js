import axios from 'axios';

const TOKEN_KEY = 'sb_token';
const USER_KEY = 'sb_user';

// VITE_API_URL non défini (dev) -> localhost:8080. Défini mais vide (build derrière nginx)
// -> baseURL relative '' : les appels /api et /uploads passent par le même origine (proxy nginx).
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
});

// -------------------------------------------------------------------------------------------------
// Boutique active (X-Shop-Id). Source de vérité posée par le ShopContext.
// L'en-tête n'est ajouté QUE si une boutique est active (mode "boutique" : OWNER/VENDOR) ET que la
// route est SCOPÉE par tenant. On l'EXCLUT des routes d'authentification et des routes MARKETPLACE
// publiques (/api/shops/**) dont le tenant est résolu par le SLUG, pas par X-Shop-Id (gate 2).
// -------------------------------------------------------------------------------------------------
let activeShopId = null;
export function setActiveShopId(id) {
  activeShopId = id != null ? String(id) : null;
}
export function getActiveShopId() {
  return activeShopId;
}

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;

  const url = config.url || '';
  const isAuthRoute = url.includes('/api/auth/');
  const isMarketplaceRoute = url.includes('/api/shops'); // catalogue/galerie/commandes -> tenant par slug
  if (activeShopId != null && !isAuthRoute && !isMarketplaceRoute) {
    config.headers['X-Shop-Id'] = activeShopId;
  }
  return config;
});

// 401 = session expirée UNIQUEMENT si on était déjà authentifié (token présent) et que l'appel
// n'est pas un endpoint d'authentification. Sinon (login mauvais mot de passe -> 401) on laisse
// l'erreur remonter au composant. Un 403 (ex. X-Shop-Id d'une boutique dont on n'est pas membre)
// N'EST PAS purgé : c'est une autorisation refusée, pas une session invalide.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const url = error.config?.url || '';
    const isAuthEndpoint = url.includes('/api/auth/');
    const hadToken = !!localStorage.getItem(TOKEN_KEY);

    if (status === 401 && hadToken && !isAuthEndpoint) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
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

export { TOKEN_KEY, USER_KEY };
export default client;

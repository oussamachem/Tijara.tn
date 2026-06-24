import axios from 'axios';

const TOKEN_KEY = 'sb_token';
const USER_KEY = 'sb_user';

// VITE_API_URL non défini (dev) -> localhost:8080. Défini mais vide (build derrière nginx)
// -> baseURL relative '' : les appels /api et /uploads passent par le même origine (proxy nginx).
const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
});

// Ajoute le JWT a chaque requete s'il est present.
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 = session expiree UNIQUEMENT si on etait deja authentifie (token present) et
// que l'appel n'est pas un endpoint d'authentification. Sinon (ex. login avec mauvais
// mot de passe -> 401), on laisse l'erreur remonter au composant pour afficher le message,
// sans purge ni redirection.
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

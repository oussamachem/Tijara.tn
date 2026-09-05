import Keycloak from 'keycloak-js';

/**
 * Adaptateur Keycloak (OIDC / SSO) — SOURCE DE VÉRITÉ de l'identité pour le back-office.
 *
 * L'app ne gère plus de mot de passe : l'utilisateur est redirigé vers la page Keycloak (Authorization
 * Code + PKCE S256). Au retour, keycloak-js détient l'access token (rafraîchi silencieusement).
 *
 * Config externalisée (prod derrière nginx : pointer VITE_KEYCLOAK_URL sur le Keycloak public).
 */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'smartboutique',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'smartboutique-web',
});

let initPromise = null;

/**
 * Initialise l'adaptateur UNE seule fois. `check-sso` (et non `login-required`) : on NE force PAS la
 * connexion — la marketplace reste publique. Si une session SSO existe déjà, l'utilisateur est
 * reconnu silencieusement ; sinon il reste anonyme jusqu'à ce qu'il clique « Se connecter ».
 */
export function initKeycloak() {
  if (initPromise) return initPromise;
  initPromise = keycloak
    .init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
      checkLoginIframe: false, // évite les soucis de cookies tiers ; le refresh token suffit
    })
    .catch((err) => {
      // Keycloak injoignable : on démarre en mode anonyme plutôt que de bloquer tout le site.
      console.warn('[keycloak] init impossible, mode anonyme:', err);
      return false;
    });
  return initPromise;
}

/**
 * Renvoie un access token FRAIS (rafraîchi s'il expire dans < 30 s). À appeler juste avant chaque
 * requête API. Renvoie null si non authentifié ou si le refresh échoue (session expirée).
 */
export async function freshToken() {
  if (!keycloak.authenticated) return null;
  try {
    await keycloak.updateToken(30);
    return keycloak.token ?? null;
  } catch {
    return null; // refresh token expiré -> l'appelant traitera le 401
  }
}

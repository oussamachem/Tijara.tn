import Keycloak from 'keycloak-js';

/**
 * Adaptateur Keycloak (OIDC / SSO) pour la marketplace autonome.
 *
 * Même client/realm que le back-office : un utilisateur déjà connecté ailleurs est reconnu
 * silencieusement (SSO). L'app ne gère plus de mot de passe : redirection vers la page Keycloak
 * (Authorization Code + PKCE S256).
 */
export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'smartboutique',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? 'smartboutique-web',
});

let initPromise = null;

/** Initialise l'adaptateur UNE fois. `check-sso` : la marketplace reste publique (pas de login forcé). */
export function initKeycloak() {
  if (initPromise) return initPromise;
  initPromise = keycloak
    .init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      pkceMethod: 'S256',
      checkLoginIframe: false,
    })
    .catch((err) => {
      console.warn('[keycloak] init impossible, mode anonyme:', err);
      return false;
    });
  return initPromise;
}

/** Access token FRAIS (rafraîchi s'il expire dans < 30 s) ou null si non authentifié / refresh échoué. */
export async function freshToken() {
  if (!keycloak.authenticated) return null;
  try {
    await keycloak.updateToken(30);
    return keycloak.token ?? null;
  } catch {
    return null;
  }
}

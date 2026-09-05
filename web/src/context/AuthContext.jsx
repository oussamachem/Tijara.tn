import { createContext, useContext, useEffect, useState } from 'react';
import { keycloak } from '../auth/keycloak.js';

const AuthContext = createContext(null);

/**
 * Construit l'objet `user` applicatif à partir des claims du token Keycloak.
 * Le back-office n'a besoin que de l'email (affichage) et du flag admin plateforme
 * (rôle realm « admin »). Les rôles de boutique restent résolus côté serveur (shop_members).
 */
function userFromToken() {
  if (!keycloak.authenticated || !keycloak.tokenParsed) return null;
  const t = keycloak.tokenParsed;
  const roles = t.realm_access?.roles ?? [];
  return {
    email: t.email || t.preferred_username || '',
    fullName: t.name || t.preferred_username || '',
    platformAdmin: roles.includes('admin'),
    roles,
  };
}

/**
 * Auth = IDENTITÉ (compte global unique), fournie par Keycloak (SSO / PKCE). Le login ne préjuge
 * d'AUCUN rôle de boutique : c'est le ShopContext (GET /api/me/shops) qui décide de l'espace.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(userFromToken);

  // Synchronise `user` sur les évènements de cycle de vie du token (login au retour de redirection,
  // refresh, expiration). keycloak-js appelle ces hooks ; on relit les claims à chaque fois.
  useEffect(() => {
    const sync = () => setUser(userFromToken());
    keycloak.onAuthSuccess = sync;
    keycloak.onAuthRefreshSuccess = sync;
    keycloak.onAuthLogout = () => setUser(null);
    sync();
    return () => {
      keycloak.onAuthSuccess = undefined;
      keycloak.onAuthRefreshSuccess = undefined;
      keycloak.onAuthLogout = undefined;
    };
  }, []);

  // Redirige vers la page de connexion Keycloak (thème personnalisé). `redirectUri` = où revenir.
  const login = (from) =>
    keycloak.login({ redirectUri: window.location.origin + (from || '/') });

  // Page d'inscription Keycloak (self-registration activée dans le realm).
  const register = () =>
    keycloak.register({ redirectUri: window.location.origin + '/' });

  // Déconnexion SSO globale (invalide la session Keycloak, pas seulement le token local).
  const logout = () => {
    localStorage.removeItem('sb_ctx'); // contexte de travail (ShopContext)
    keycloak.logout({ redirectUri: window.location.origin + '/login' });
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isPlatformAdmin: !!user?.platformAdmin,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}

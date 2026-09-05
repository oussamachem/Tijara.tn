import { createContext, useContext, useEffect, useState } from 'react';
import { keycloak } from './keycloak.js';

const AuthContext = createContext(null);

/** Objet `user` dérivé des claims du token Keycloak (email + nom, suffisants pour la marketplace). */
function userFromToken() {
  if (!keycloak.authenticated || !keycloak.tokenParsed) return null;
  const t = keycloak.tokenParsed;
  return {
    email: t.email || t.preferred_username || '',
    fullName: t.name || t.preferred_username || '',
  };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(userFromToken);

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

  // Redirige vers la page Keycloak (thème Smart Boutique). `from` = chemin de retour.
  const login = (from) =>
    keycloak.login({ redirectUri: window.location.origin + (from || '/') });

  const register = () =>
    keycloak.register({ redirectUri: window.location.origin + '/' });

  const logout = () =>
    keycloak.logout({ redirectUri: window.location.origin + '/' });

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

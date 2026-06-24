import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as SecureStore from 'expo-secure-store';
import { authApi } from '../api/endpoints';
import { TOKEN_KEY, setUnauthorizedHandler } from '../api/client';

const USER_KEY = 'sb_user';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [hydrating, setHydrating] = useState(true);

  const logout = useCallback(async () => {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    await SecureStore.deleteItemAsync(USER_KEY);
    setUser(null);
  }, []);

  // Hydratation au démarrage depuis le SecureStore (Keychain / Keystore).
  useEffect(() => {
    (async () => {
      try {
        const token = await SecureStore.getItemAsync(TOKEN_KEY);
        const raw = await SecureStore.getItemAsync(USER_KEY);
        if (token && raw) setUser(JSON.parse(raw));
      } catch {
        // ignore : on démarre déconnecté
      } finally {
        setHydrating(false);
      }
    })();
  }, []);

  // Un 401 sur un appel authentifié (session expirée) déclenche la déconnexion.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout();
    });
  }, [logout]);

  const login = async (email, password) => {
    const { data } = await authApi.login(email, password);
    await SecureStore.setItemAsync(TOKEN_KEY, data.token);
    await SecureStore.setItemAsync(USER_KEY, JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  };

  return (
    <AuthContext.Provider value={{ user, hydrating, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return ctx;
}

import { createContext, useContext, useState } from 'react';
import { authApi } from '../api/endpoints';
import { TOKEN_KEY, USER_KEY } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
  });

  const persist = (token, u) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
  };

  const login = async (email, password) => {
    const { data } = await authApi.login(email, password);
    persist(data.token, data.user);
    return data.user;
  };

  const register = async (fullName, email, password) => {
    const { data } = await authApi.register(fullName, email, password);
    persist(data.token, data.user);
    return data.user;
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

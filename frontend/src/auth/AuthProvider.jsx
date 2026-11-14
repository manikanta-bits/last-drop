import { useCallback, useEffect, useMemo, useState } from 'react';
import api, { TOKEN_KEY } from '../api/client.js';
import { AuthContext } from './useAuth.js';

// Wraps the whole app (see main.jsx) and keeps track of the logged-in user.
// Pages read it with useAuth() from useAuth.js.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // true while we check a saved token with the backend on first load
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem(TOKEN_KEY)));

  // On page load: if a token was saved earlier, ask the backend who it belongs to.
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return;
    api.get('/api/auth/me')
      .then((response) => setUser(response.data))
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false));
  }, []);

  const saveSession = (data) => {
    localStorage.setItem(TOKEN_KEY, data.token);
    setUser(data.user);
    return data.user;
  };

  const login = useCallback(async (email, password) => {
    const response = await api.post('/api/auth/login', { email, password });
    return saveSession(response.data);
  }, []);

  const register = useCallback(async (form) => {
    const response = await api.post('/api/auth/register', form);
    return saveSession(response.data);
  }, []);

  // "Continue with Google": credential is the token Google gives the browser
  const loginWithGoogle = useCallback(async (credential, role) => {
    const response = await api.post('/api/auth/google', { credential, role });
    return saveSession(response.data);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, loginWithGoogle, logout }),
    [user, loading, login, register, loginWithGoogle, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}


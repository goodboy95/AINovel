import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { validateToken as apiValidateToken } from '../services/api';

type AuthUser = {
  username: string;
};

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (token: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [didInit, setDidInit] = useState(false);

  const performValidate = async () => {
    try {
      const data = await apiValidateToken();
      setUser({ username: data.username });
      setIsAuthenticated(true);
    } catch {
      localStorage.removeItem('token');
      setUser(null);
      setIsAuthenticated(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (didInit) return;
    setDidInit(true);
    const token = localStorage.getItem('token');
    if (token) {
      performValidate();
    } else {
      setLoading(false);
    }
  }, [didInit]);

  const login = async (token: string) => {
    localStorage.setItem('token', token);
    setLoading(true);
    await performValidate();
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = useMemo(
    () => ({ user, isAuthenticated, loading, login, logout }),
    [user, isAuthenticated, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
};

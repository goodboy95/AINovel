import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { validateToken as apiValidateToken } from '../services/api';
import type { PermissionMap } from '../types';

type AuthUser = {
  username: string;
  id?: number;
  workspaceId?: number;
};

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (token: string) => Promise<void>;
  logout: () => void;
  permissions: PermissionMap;
  workspaceId?: number | null;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [didInit, setDidInit] = useState(false);
  const [permissions, setPermissions] = useState<PermissionMap>({});
  const [workspaceId, setWorkspaceId] = useState<number | null>(null);

  const performValidate = useCallback(async () => {
    try {
      const data = await apiValidateToken();
      setUser({ username: data.username, id: data.userId, workspaceId: data.workspaceId });
      setPermissions(data.permissions ?? {});
      setWorkspaceId(data.workspaceId ?? null);
      setIsAuthenticated(true);
    } catch {
      localStorage.removeItem('token');
      setPermissions({});
      setWorkspaceId(null);
      setUser(null);
      setIsAuthenticated(false);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (didInit) return;
    setDidInit(true);
    const token = localStorage.getItem('token');
    if (token) {
      performValidate();
    } else {
      setLoading(false);
    }
  }, [didInit, performValidate]);

  const login = useCallback(async (token: string) => {
    localStorage.setItem('token', token);
    setLoading(true);
    await performValidate();
  }, [performValidate]);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    setUser(null);
    setIsAuthenticated(false);
    setPermissions({});
    setWorkspaceId(null);
  }, []);

  const value = useMemo(
    () => ({ user, isAuthenticated, loading, login, logout, permissions, workspaceId }),
    [user, isAuthenticated, loading, login, logout, permissions, workspaceId]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
};

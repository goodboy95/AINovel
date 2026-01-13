import React, { createContext, useContext, useState, useEffect } from "react";
import { User } from "@/types";
import { api } from "@/lib/mock-api";

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  acceptToken: (token: string) => void;
  logout: () => void;
  refreshProfile: () => Promise<void>; // V2: Refresh credits/check-in status
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const initAuth = async () => {
    const token = localStorage.getItem("token");
    if (token) {
      try {
        const userData = await api.user.getProfile();
        setUser(userData);
      } catch (error) {
        console.error("Auth validation failed", error);
        localStorage.removeItem("token");
      }
    }
    setIsLoading(false);
  };

  useEffect(() => {
    initAuth();
  }, []);

  const acceptToken = (token: string) => {
    localStorage.setItem("token", token);
    setUser(null);
  };

  const logout = () => {
    localStorage.removeItem("token");
    setUser(null);
  };

  const refreshProfile = async () => {
    try {
      const updatedUser = await api.user.getProfile();
      setUser(updatedUser);
    } catch (error) {
      console.error("Failed to refresh profile", error);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'admin',
        isLoading,
        acceptToken,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

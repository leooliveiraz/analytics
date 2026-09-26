import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, hasTokens, setTokens } from "../api/client";
import type { Tokens, User } from "../api/types";

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name?: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    async function load() {
      if (!hasTokens()) {
        setLoading(false);
        return;
      }
      try {
        const me = await api<User>("/api/v1/auth/me");
        if (active) {
          setUser(me);
        }
      } catch {
        setTokens(null, null);
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, []);

  const applyTokens = useCallback(async (tokens: Tokens) => {
    setTokens(tokens.accessToken, tokens.refreshToken);
    setUser(await api<User>("/api/v1/auth/me"));
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const tokens = await api<Tokens>("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      await applyTokens(tokens);
    },
    [applyTokens],
  );

  const register = useCallback(
    async (email: string, password: string, name?: string) => {
      const tokens = await api<Tokens>("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify({ email, password, name }),
      });
      await applyTokens(tokens);
    },
    [applyTokens],
  );

  const logout = useCallback(async () => {
    try {
      await api<void>("/api/v1/auth/logout", { method: "POST" });
    } catch {
      /* ignore */
    }
    setTokens(null, null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient } from "@/lib/http/api-client";
import { clearSession, loadSession, saveSession } from "@/features/auth/lib/session";
import type { AuthSession } from "@/features/auth/model/auth-types";

interface AuthContextValue {
  session: AuthSession | null;
  signIn: (token: string) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession());
  const navigate = useNavigate();

  useEffect(() => {
    const response = apiClient.interceptors.response.use(
      (value) => value,
      (error: { response?: { status?: number } }) => {
        if (error.response?.status === 401 && session) {
          clearSession();
          setSession(null);
          const returnTo = `${location.pathname}${location.search}`;
          navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`, { replace: true });
        }
        return Promise.reject(error);
      },
    );
    return () => {
      apiClient.interceptors.response.eject(response);
    };
  }, [navigate, session]);

  const value = useMemo<AuthContextValue>(() => ({
    session,
    signIn: (token) => setSession(saveSession(token)),
    signOut: () => {
      clearSession();
      setSession(null);
      navigate("/login", { replace: true });
    },
  }), [navigate, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Auth hook and provider share the private context by design.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth phải nằm trong AuthProvider.");
  return context;
}

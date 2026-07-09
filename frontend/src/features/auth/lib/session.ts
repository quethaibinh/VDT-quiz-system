import type { AuthSession } from "@/features/auth/model/auth-types";
import { decodeAuthToken, isExpired } from "@/features/auth/lib/jwt";

const KEY = "sahara.quiz.session";

export function loadSession(): AuthSession | null {
  const token = sessionStorage.getItem(KEY);
  if (!token) return null;
  try {
    const claims = decodeAuthToken(token);
    if (isExpired(claims)) {
      sessionStorage.removeItem(KEY);
      return null;
    }
    return { token, claims };
  } catch {
    sessionStorage.removeItem(KEY);
    return null;
  }
}

export function saveSession(token: string): AuthSession {
  const session = { token, claims: decodeAuthToken(token) };
  sessionStorage.setItem(KEY, token);
  return session;
}

export function clearSession() {
  sessionStorage.removeItem(KEY);
}

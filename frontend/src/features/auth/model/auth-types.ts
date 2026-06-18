export type UserRole = "ADMIN" | "TEACHER" | "STUDENT";

export interface AuthClaims {
  userId: string;
  userRole: UserRole;
  username: string;
  displayName?: string | null;
  fullName?: string | null;
  exp: number;
  iat?: number;
}

export interface AuthSession {
  token: string;
  claims: AuthClaims;
}

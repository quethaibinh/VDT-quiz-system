import { decodeJwt } from "jose";
import type { AuthClaims, UserRole } from "@/features/auth/model/auth-types";

const roles: UserRole[] = ["ADMIN", "TEACHER", "STUDENT"];

export function decodeAuthToken(token: string): AuthClaims {
  const claims = decodeJwt(token);
  const role = String(claims.userRole ?? "") as UserRole;
  if (!claims.exp || !claims.userId || !claims.username || !roles.includes(role)) {
    throw new Error("Token không chứa đầy đủ thông tin người dùng.");
  }
  return {
    userId: String(claims.userId),
    userRole: role,
    username: String(claims.username),
    displayName: claims.displayName ? String(claims.displayName) : null,
    fullName: claims.fullName ? String(claims.fullName) : null,
    exp: claims.exp,
    iat: claims.iat,
  };
}

export function isExpired(claims: AuthClaims, toleranceSeconds = 0) {
  return claims.exp <= Math.floor(Date.now() / 1000) + toleranceSeconds;
}

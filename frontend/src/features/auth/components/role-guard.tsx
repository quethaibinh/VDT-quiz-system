import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import type { UserRole } from "@/features/auth/model/auth-types";

export function RoleGuard({ role }: { role: UserRole }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }
  if (session.claims.userRole !== role) return <Navigate to="/forbidden" replace />;
  return <Outlet />;
}

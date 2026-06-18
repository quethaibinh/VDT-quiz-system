import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export function TeacherGuard() {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) return <Navigate to={`/login?returnTo=${encodeURIComponent(location.pathname + location.search)}`} replace />;
  if (session.claims.userRole !== "TEACHER") return <Navigate to="/forbidden" replace />;
  return <Outlet />;
}

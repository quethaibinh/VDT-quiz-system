import type { UserRole } from "@/features/auth/model/auth-types";

export function getRoleHome(role: UserRole) {
  if (role === "ADMIN") return "/admin/dashboard";
  if (role === "TEACHER") return "/teacher/subjects";
  return "/forbidden";
}

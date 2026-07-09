import type { UserRole } from "@/features/auth/model/auth-types";

// Lay trang chu tuong ung voi vai tro nguoi dung
export function getRoleHome(role: UserRole) {
  if (role === "ADMIN") return "/admin/dashboard";
  if (role === "TEACHER") return "/teacher/subjects";
  if (role === "STUDENT") return "/student/dashboard";
  return "/forbidden";
}

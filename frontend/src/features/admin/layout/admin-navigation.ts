import { BookOpen, LayoutDashboard, Upload, Users } from "lucide-react";
import type { WorkspaceNavigationItem } from "@/components/shared/workspace-shell";

export const adminNavigationItems: WorkspaceNavigationItem[] = [
  { label: "Tổng quan", icon: LayoutDashboard, path: "/admin/dashboard", active: (path) => path.startsWith("/admin/dashboard") },
  { label: "Người dùng", icon: Users, path: "/admin/users", active: (path) => path === "/admin/users" || /^\/admin\/users\/(?!import)/.test(path) },
  { label: "Import tài khoản", icon: Upload, path: "/admin/users/import", active: (path) => path.startsWith("/admin/users/import") },
  { label: "Môn học", icon: BookOpen, path: "/admin/subjects", active: (path) => path.startsWith("/admin/subjects") },
];

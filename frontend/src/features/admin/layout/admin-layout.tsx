import { Outlet } from "react-router-dom";
import { WorkspaceShell } from "@/components/shared/workspace-shell";
import { adminNavigationItems } from "@/features/admin/layout/admin-navigation";

export function AdminLayout() {
  return <WorkspaceShell homePath="/admin/dashboard" workspaceLabel="Không gian quản trị" roleLabel="Quản trị viên" navigation={adminNavigationItems}><Outlet /></WorkspaceShell>;
}

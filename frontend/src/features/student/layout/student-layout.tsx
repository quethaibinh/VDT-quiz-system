import { Outlet } from "react-router-dom";
import { WorkspaceShell } from "@/components/shared/workspace-shell";
import { getStudentSection, studentNavigationItems } from "@/features/student/layout/student-navigation";

// Layout chung cho phan he hoc sinh
export function StudentLayout() {
  return (
    <WorkspaceShell
      homePath="/student/dashboard"
      workspaceLabel="Không gian học sinh"
      roleLabel="Học sinh"
      navigation={studentNavigationItems.map((item) => ({
        ...item,
        active: (pathname) => getStudentSection(pathname) === item.section,
      }))}
    >
      <Outlet />
    </WorkspaceShell>
  );
}

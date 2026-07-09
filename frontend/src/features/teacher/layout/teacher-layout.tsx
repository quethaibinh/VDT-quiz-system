import { Outlet } from "react-router-dom";
import { WorkspaceShell } from "@/components/shared/workspace-shell";
import { getTeacherSection, teacherNavigationItems } from "@/features/teacher/layout/teacher-navigation";

export function TeacherLayout() {
  return (
    <WorkspaceShell
      homePath="/teacher/subjects"
      workspaceLabel="Không gian giáo viên"
      roleLabel="Giáo viên"
      navigation={teacherNavigationItems.map((item) => ({
        ...item,
        active: (pathname) => getTeacherSection(pathname) === item.section,
      }))}
    >
      <Outlet />
    </WorkspaceShell>
  );
}

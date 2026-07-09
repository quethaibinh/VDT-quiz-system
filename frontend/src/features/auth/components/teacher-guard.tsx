import { RoleGuard } from "@/features/auth/components/role-guard";

export function TeacherGuard() {
  return <RoleGuard role="TEACHER" />;
}

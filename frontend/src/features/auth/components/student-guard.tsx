import { RoleGuard } from "@/features/auth/components/role-guard";

// Guard kiem tra quyen truy cap cua hoc sinh
export function StudentGuard() {
  return <RoleGuard role="STUDENT" />;
}

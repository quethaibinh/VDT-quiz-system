import { RoleGuard } from "@/features/auth/components/role-guard";

export function AdminGuard() {
  return <RoleGuard role="ADMIN" />;
}

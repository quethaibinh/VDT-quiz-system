import type { PageResponse } from "@/lib/api-types";

export type ManagedUserRole = "TEACHER" | "STUDENT";
export type UserStatus = "ACTIVE" | "INACTIVE";
export type UserGender = "MALE" | "FEMALE" | "OTHER";

export interface AdminUserSummary {
  id: string;
  username: string;
  userType: ManagedUserRole;
  status: UserStatus;
  fullName: string;
  displayName?: string | null;
  email?: string | null;
  studentCode?: string | null;
  teacherCode?: string | null;
  createdAt?: string | null;
}

export interface AdminUserDetail extends AdminUserSummary {
  birthDate?: string | null;
  gender?: UserGender | null;
  updatedAt?: string | null;
}

export type AdminUserPage = PageResponse<AdminUserSummary>;

export interface AdminUserStatistics {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  activeTeachers: number;
  activeStudents: number;
}

export interface UpdateAdminUserInput {
  fullName: string;
  displayName: string | null;
  email: string | null;
  birthDate: string | null;
  gender: UserGender | null;
}

export interface AdminUserFilters {
  keyword?: string;
  userType?: ManagedUserRole;
  status?: UserStatus;
  page: number;
  size: number;
  sort: string;
}

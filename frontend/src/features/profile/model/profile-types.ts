import type { UserRole } from "@/features/auth/model/auth-types";

export interface UserProfile {
  id: string;
  username: string;
  userType: UserRole;
  studentCode: string | null;
  teacherCode: string | null;
  fullName: string;
  displayName: string | null;
  email: string | null;
  birthDate: string | null; // yyyy-MM-dd
  gender: "MALE" | "FEMALE" | "OTHER" | null;
  avatarUrl: string | null;
}

export interface UpdateUserProfileRequest {
  fullName: string;
  displayName: string;
  email: string;
  birthDate: string;
  gender: "MALE" | "FEMALE" | "OTHER";
}

export interface UpdatePasswordRequest {
  currentPassword?: string;
  newPassword?: string;
  confirmPassword?: string;
}

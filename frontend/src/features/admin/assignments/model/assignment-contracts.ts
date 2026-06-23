export type AssignmentIdentityState = "RESOLVED" | "MISSING" | "NON_TEACHER";

export interface SubjectTeacherAssignment {
  id: string;
  subjectId: string;
  teacherId: string;
  status: string;
  assignedByAdminId: string;
  assignedAt: string;
  teacherCode?: string | null;
  fullName?: string | null;
  displayName?: string | null;
  email?: string | null;
  teacherStatus?: "ACTIVE" | "INACTIVE" | null;
  identityState?: AssignmentIdentityState;
}

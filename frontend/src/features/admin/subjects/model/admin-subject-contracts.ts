export type SubjectStatus = "ACTIVE" | "ARCHIVED";

export interface AdminSubject {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: SubjectStatus;
  createdByAdminId?: string;
  updatedByAdminId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminSubjectInput {
  code: string;
  name: string;
  description: string | null;
}

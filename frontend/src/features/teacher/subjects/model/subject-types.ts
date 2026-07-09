export interface Subject {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  status: "ACTIVE" | "ARCHIVED";
  createdByAdminId?: string | null;
  updatedByAdminId?: string | null;
  createdAt: string;
  updatedAt: string;
  assignedAt?: string | null;
}

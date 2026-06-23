import { queryOptions } from "@tanstack/react-query";
import { getAdminSubject, listAdminSubjects } from "@/features/admin/subjects/api/admin-subject-api";
import type { SubjectStatus } from "@/features/admin/subjects/model/admin-subject-contracts";

export const adminSubjectKeys = {
  all: ["admin", "subjects"] as const,
  list: (keyword = "", status = "") => [...adminSubjectKeys.all, "list", keyword, status] as const,
  detail: (id: string) => [...adminSubjectKeys.all, "detail", id] as const,
};
export const adminSubjectListQuery = (keyword = "", status?: SubjectStatus) => queryOptions({
  queryKey: adminSubjectKeys.list(keyword, status),
  queryFn: () => listAdminSubjects({ keyword: keyword || undefined, status }),
});
export const adminSubjectDetailQuery = (id: string) => queryOptions({
  queryKey: adminSubjectKeys.detail(id),
  queryFn: () => getAdminSubject(id),
  enabled: Boolean(id),
});

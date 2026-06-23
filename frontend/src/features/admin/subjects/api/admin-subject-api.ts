import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { AdminSubject, AdminSubjectInput, SubjectStatus } from "@/features/admin/subjects/model/admin-subject-contracts";

const base = "/v1/api/admin/question-service/subjects";

export async function listAdminSubjects(params?: { keyword?: string; status?: SubjectStatus }) {
  const response = await apiClient.get<ApiResponse<AdminSubject[]>>(base, { params });
  return unwrap(response.data);
}
export async function getAdminSubject(id: string) {
  const response = await apiClient.get<ApiResponse<AdminSubject>>(`${base}/${id}`);
  return unwrap(response.data);
}
export async function createAdminSubject(input: AdminSubjectInput) {
  const response = await apiClient.post<ApiResponse<AdminSubject>>(base, input);
  return unwrap(response.data);
}
export async function updateAdminSubject(id: string, input: AdminSubjectInput) {
  const response = await apiClient.put<ApiResponse<AdminSubject>>(`${base}/${id}`, input);
  return unwrap(response.data);
}
export async function archiveAdminSubject(id: string) {
  const response = await apiClient.patch<ApiResponse<AdminSubject>>(`${base}/${id}/archive`);
  return unwrap(response.data);
}
export async function restoreAdminSubject(id: string) {
  const response = await apiClient.patch<ApiResponse<AdminSubject>>(`${base}/${id}/restore`);
  return unwrap(response.data);
}

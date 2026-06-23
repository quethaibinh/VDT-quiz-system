import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { SubjectTeacherAssignment } from "@/features/admin/assignments/model/assignment-contracts";

const base = (subjectId: string) => `/v1/api/admin/question-service/subjects/${subjectId}/teachers`;
export async function listSubjectTeachers(subjectId: string) {
  const response = await apiClient.get<ApiResponse<SubjectTeacherAssignment[]>>(base(subjectId));
  return unwrap(response.data);
}
export async function assignSubjectTeacher(subjectId: string, teacherId: string) {
  const response = await apiClient.post<ApiResponse<SubjectTeacherAssignment>>(base(subjectId), { teacherId });
  return unwrap(response.data);
}
export async function removeSubjectTeacher(subjectId: string, teacherId: string) {
  await apiClient.delete(`${base(subjectId)}/${teacherId}`);
}

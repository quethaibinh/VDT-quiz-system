import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { Subject } from "@/features/teacher/subjects/model/subject-types";

export async function listSubjects(params?: { status?: string; keyword?: string }) {
  const response = await apiClient.get<ApiResponse<Subject[]>>("/v1/api/question-service/teacher/subjects", { params });
  return unwrap(response.data);
}

export async function getSubject(subjectId: string) {
  const response = await apiClient.get<ApiResponse<Subject>>(`/v1/api/question-service/teacher/subjects/${subjectId}`);
  return unwrap(response.data);
}

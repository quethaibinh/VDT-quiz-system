import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { ImportQuestionResult } from "@/features/teacher/imports/model/import-types";

export async function importQuestions(subjectId: string, file: File) {
  const form = new FormData();
  form.append("file", file);
  const response = await apiClient.post<ApiResponse<ImportQuestionResult>>(`/v1/api/question-service/teacher/subjects/${subjectId}/questions/import`, form);
  return unwrap(response.data);
}

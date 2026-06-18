import { apiClient } from "@/lib/http/api-client";
import type { ExamDraftInput, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

export interface ExamListParams {
  subjectId: string;
  status?: ExamSummary["status"];
}

export async function listExams(params: ExamListParams) {
  return (await apiClient.get<ExamSummary[]>("/v1/api/exam-service/teacher/exams", { params })).data;
}
export async function createExam(input: ExamDraftInput) {
  return (await apiClient.post<ExamSummary>("/v1/api/exam-service/teacher/exams", input)).data;
}
export async function activateExam(id: string) {
  return (await apiClient.post<ExamSummary>(`/v1/api/exam-service/teacher/exams/${id}/activate`)).data;
}

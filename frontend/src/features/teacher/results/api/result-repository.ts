import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  PublishResultsResponse,
  TeacherExamResults,
  TeacherResultDetail,
  TeacherResultRow,
} from "@/features/teacher/results/model/result-contracts";

const root = (examId: string) => `/v1/api/result-service/teacher/exams/${examId}/results`;

export async function getTeacherExamResults(
  examId: string,
  params: { page?: number; size?: number } = {},
): Promise<TeacherExamResults> {
  const response = await apiClient.get<ApiResponse<TeacherExamResults>>(root(examId), { params });
  return unwrap(response.data);
}

export async function getTeacherResultDetail(examId: string, resultId: string): Promise<TeacherResultDetail> {
  const response = await apiClient.get<ApiResponse<TeacherResultDetail>>(`${root(examId)}/${resultId}`);
  return unwrap(response.data);
}

export async function adjustTeacherResultScore(
  examId: string,
  resultId: string,
  input: { adjustedScore: number; reason: string },
): Promise<TeacherResultRow> {
  const response = await apiClient.patch<ApiResponse<TeacherResultRow>>(`${root(examId)}/${resultId}/score`, input);
  return unwrap(response.data);
}

export async function publishTeacherResult(examId: string, resultId: string): Promise<PublishResultsResponse> {
  const response = await apiClient.post<ApiResponse<PublishResultsResponse>>(`${root(examId)}/${resultId}/publish`);
  return unwrap(response.data);
}

export async function publishTeacherExamResults(examId: string, resultIds?: string[]): Promise<PublishResultsResponse> {
  const response = await apiClient.post<ApiResponse<PublishResultsResponse>>(`${root(examId)}/publish`, { resultIds });
  return unwrap(response.data);
}

export async function exportTeacherExamResults(examId: string): Promise<Blob> {
  const response = await apiClient.get(`${root(examId)}/export`, { responseType: "blob" });
  return response.data as Blob;
}


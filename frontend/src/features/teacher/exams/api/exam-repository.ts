import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  ExamDetail,
  ExamDraftRequest,
  ExamStatus,
  ExamSummary,
} from "@/features/teacher/exams/model/exam-contracts";

export interface ExamListParams {
  status?: ExamStatus;
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const root = (subjectId: string) =>
  `/v1/api/exam-service/teacher/subjects/${subjectId}/exams`;

export const examKeys = {
  all: ["teacher", "subjects"] as const,
  subject: (subjectId: string) => [...examKeys.all, subjectId, "exams"] as const,
  lists: (subjectId: string) => [...examKeys.subject(subjectId), "list"] as const,
  list: (subjectId: string, params: ExamListParams = {}) =>
    [...examKeys.lists(subjectId), params] as const,
  details: (subjectId: string) => [...examKeys.subject(subjectId), "detail"] as const,
  detail: (subjectId: string, examId: string) =>
    [...examKeys.details(subjectId), examId] as const,
};

export async function listExams(
  subjectId: string,
  params: ExamListParams = {},
): Promise<PageResponse<ExamSummary>> {
  const response = await apiClient.get<ApiResponse<PageResponse<ExamSummary>>>(root(subjectId), {
    params,
  });
  return unwrap(response.data);
}

export async function getExam(subjectId: string, examId: string): Promise<ExamDetail> {
  const response = await apiClient.get<ApiResponse<ExamDetail>>(`${root(subjectId)}/${examId}`);
  return unwrap(response.data);
}

export async function createExam(
  subjectId: string,
  input: ExamDraftRequest,
): Promise<ExamDetail> {
  const response = await apiClient.post<ApiResponse<ExamDetail>>(root(subjectId), input);
  return unwrap(response.data);
}

export async function updateExam(
  subjectId: string,
  examId: string,
  input: ExamDraftRequest,
): Promise<ExamDetail> {
  const response = await apiClient.put<ApiResponse<ExamDetail>>(
    `${root(subjectId)}/${examId}`,
    input,
  );
  return unwrap(response.data);
}

export async function cancelExam(subjectId: string, examId: string): Promise<ExamDetail> {
  const response = await apiClient.patch<ApiResponse<ExamDetail>>(
    `${root(subjectId)}/${examId}/cancel`,
  );
  return unwrap(response.data);
}

export async function scheduleExam(subjectId: string, examId: string): Promise<ExamDetail> {
  const response = await apiClient.patch<ApiResponse<ExamDetail>>(
    `${root(subjectId)}/${examId}/schedule`,
  );
  return unwrap(response.data);
}

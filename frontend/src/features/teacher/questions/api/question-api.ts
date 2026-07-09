import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { Question, QuestionFilters, QuestionDetail, QuestionInput, Topic } from "@/features/teacher/questions/model/question-types";

const root = (subjectId: string) => `/v1/api/question-service/teacher/subjects/${subjectId}`;

export async function searchQuestions(subjectId: string, filters: QuestionFilters) {
  const response = await apiClient.get<ApiResponse<PageResponse<Question>>>(`${root(subjectId)}/questions`, { params: filters });
  return unwrap(response.data);
}

export async function createQuestion(subjectId: string, input: QuestionInput) {
  const response = await apiClient.post<ApiResponse<QuestionDetail>>(`${root(subjectId)}/questions`, input);
  return unwrap(response.data);
}

export async function getQuestionDetail(subjectId: string, id: string) {
  const response = await apiClient.get<ApiResponse<QuestionDetail>>(`${root(subjectId)}/questions/${id}`);
  return unwrap(response.data);
}

export async function updateQuestion(subjectId: string, id: string, input: QuestionInput) {
  const response = await apiClient.put<ApiResponse<QuestionDetail>>(`${root(subjectId)}/questions/${id}`, input);
  return unwrap(response.data);
}

export async function archiveQuestion(subjectId: string, id: string) {
  const response = await apiClient.patch<ApiResponse<QuestionDetail>>(`${root(subjectId)}/questions/${id}/archive`);
  return unwrap(response.data);
}

export async function restoreQuestion(subjectId: string, id: string) {
  const response = await apiClient.patch<ApiResponse<QuestionDetail>>(`${root(subjectId)}/questions/${id}/restore`);
  return unwrap(response.data);
}

export async function listTopics(subjectId: string) {
  const response = await apiClient.get<ApiResponse<Topic[]>>(`${root(subjectId)}/topics`);
  return unwrap(response.data);
}

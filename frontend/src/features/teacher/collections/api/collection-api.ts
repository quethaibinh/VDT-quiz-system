import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { QuestionCollection, CollectionInput, BulkCollectionResult } from "@/features/teacher/collections/model/collection-types";
import type { Question, QuestionFilterParams } from "@/features/teacher/questions";

const root = (subjectId: string) => `/v1/api/question-service/teacher/subjects/${subjectId}/question-collections`;

export async function listCollections(subjectId: string, params: Record<string, unknown>) {
  const response = await apiClient.get<ApiResponse<PageResponse<QuestionCollection>>>(root(subjectId), { params });
  return unwrap(response.data);
}
export async function getCollection(subjectId: string, id: string) {
  const response = await apiClient.get<ApiResponse<QuestionCollection>>(`${root(subjectId)}/${id}`);
  return unwrap(response.data);
}
export async function createCollection(subjectId: string, input: CollectionInput) {
  const response = await apiClient.post<ApiResponse<QuestionCollection>>(root(subjectId), input);
  return unwrap(response.data);
}
export async function updateCollection(subjectId: string, id: string, input: CollectionInput) {
  const response = await apiClient.put<ApiResponse<QuestionCollection>>(`${root(subjectId)}/${id}`, input);
  return unwrap(response.data);
}
export async function archiveCollection(subjectId: string, id: string) {
  const response = await apiClient.delete<ApiResponse<QuestionCollection>>(`${root(subjectId)}/${id}`);
  return unwrap(response.data);
}
export async function restoreCollection(subjectId: string, id: string) {
  const response = await apiClient.patch<ApiResponse<QuestionCollection>>(`${root(subjectId)}/${id}/restore`);
  return unwrap(response.data);
}
export async function listCollectionQuestions(subjectId: string, id: string, filters: QuestionFilterParams) {
  const response = await apiClient.get<ApiResponse<PageResponse<Question>>>(`${root(subjectId)}/${id}/questions`, { params: filters });
  return unwrap(response.data);
}
export async function addQuestions(subjectId: string, id: string, questionIds: string[]) {
  const response = await apiClient.post<ApiResponse<BulkCollectionResult>>(`${root(subjectId)}/${id}/questions`, { questionIds });
  return unwrap(response.data);
}
export async function removeQuestions(subjectId: string, id: string, questionIds: string[]) {
  const response = await apiClient.post<ApiResponse<BulkCollectionResult>>(`${root(subjectId)}/${id}/questions/remove`, { questionIds });
  return unwrap(response.data);
}
export async function addQuestionsByFilter(subjectId: string, id: string, filters: Record<string, unknown>) {
  const response = await apiClient.post<ApiResponse<BulkCollectionResult>>(`${root(subjectId)}/${id}/questions/add-by-filter`, filters);
  return unwrap(response.data);
}

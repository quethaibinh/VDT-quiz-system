import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  LiveQuizDetail,
  LiveQuizPrepareResponse,
  LiveQuizRequest,
  LiveQuizRoom,
  LiveQuizStatus,
  LiveQuizSummary,
} from "@/features/teacher/live-quizzes/model/live-quiz-contracts";

export interface LiveQuizListParams {
  status?: LiveQuizStatus;
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const root = (subjectId: string) =>
  `/v1/api/exam-service/teacher/subjects/${subjectId}/live-quizzes`;
const roomRoot = "/v1/api/examruntime-service/teacher/live-quizzes";

export const liveQuizKeys = {
  all: ["teacher", "live-quizzes"] as const,
  subject: (subjectId: string) => [...liveQuizKeys.all, "subjects", subjectId] as const,
  lists: (subjectId: string) => [...liveQuizKeys.subject(subjectId), "list"] as const,
  list: (subjectId: string, params: LiveQuizListParams = {}) =>
    [...liveQuizKeys.lists(subjectId), params] as const,
  details: (subjectId: string) => [...liveQuizKeys.subject(subjectId), "detail"] as const,
  detail: (subjectId: string, quizId: string) =>
    [...liveQuizKeys.details(subjectId), quizId] as const,
  rooms: () => [...liveQuizKeys.all, "rooms"] as const,
  room: (roomId: string) => [...liveQuizKeys.rooms(), roomId] as const,
};

export async function listLiveQuizzes(
  subjectId: string,
  params: LiveQuizListParams = {},
): Promise<PageResponse<LiveQuizSummary>> {
  const response = await apiClient.get<ApiResponse<PageResponse<LiveQuizSummary>>>(root(subjectId), {
    params,
  });
  return unwrap(response.data);
}

export async function getLiveQuiz(subjectId: string, quizId: string): Promise<LiveQuizDetail> {
  const response = await apiClient.get<ApiResponse<LiveQuizDetail>>(`${root(subjectId)}/${quizId}`);
  return unwrap(response.data);
}

export async function createLiveQuiz(
  subjectId: string,
  input: LiveQuizRequest,
): Promise<LiveQuizDetail> {
  const response = await apiClient.post<ApiResponse<LiveQuizDetail>>(root(subjectId), input);
  return unwrap(response.data);
}

export async function updateLiveQuiz(
  subjectId: string,
  quizId: string,
  input: LiveQuizRequest,
): Promise<LiveQuizDetail> {
  const response = await apiClient.put<ApiResponse<LiveQuizDetail>>(
    `${root(subjectId)}/${quizId}`,
    input,
  );
  return unwrap(response.data);
}

export async function prepareLiveQuiz(
  subjectId: string,
  quizId: string,
): Promise<LiveQuizPrepareResponse> {
  const response = await apiClient.post<ApiResponse<LiveQuizPrepareResponse>>(
    `${root(subjectId)}/${quizId}/prepare`,
  );
  return unwrap(response.data);
}

export async function getLiveQuizRoom(roomId: string): Promise<LiveQuizRoom> {
  const response = await apiClient.get<ApiResponse<LiveQuizRoom>>(`${roomRoot}/${roomId}`);
  return unwrap(response.data);
}

export async function openLiveQuizRoom(roomId: string): Promise<LiveQuizRoom> {
  const response = await apiClient.post<ApiResponse<LiveQuizRoom>>(`${roomRoot}/${roomId}/open`);
  return unwrap(response.data);
}

export async function closeLiveQuizRoom(roomId: string): Promise<LiveQuizRoom> {
  const response = await apiClient.post<ApiResponse<LiveQuizRoom>>(`${roomRoot}/${roomId}/close`);
  return unwrap(response.data);
}

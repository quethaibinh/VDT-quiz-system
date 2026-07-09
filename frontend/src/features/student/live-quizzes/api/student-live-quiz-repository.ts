import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  StudentLiveQuizAnswerRequest,
  StudentLiveQuizAnswerResponse,
  StudentLiveQuizCurrentQuestion,
  StudentLiveQuizJoinRequest,
  StudentLiveQuizJoinResponse,
  StudentLiveQuizState,
} from "@/features/student/live-quizzes/model/live-quiz-contracts";

const root = "/v1/api/examruntime-service/student/live-quizzes";

export const studentLiveQuizKeys = {
  all: ["student", "live-quizzes"] as const,
  room: (roomId: string) => [...studentLiveQuizKeys.all, "rooms", roomId] as const,
  state: (roomId: string) => [...studentLiveQuizKeys.room(roomId), "state"] as const,
  currentQuestion: (roomId: string) => [...studentLiveQuizKeys.room(roomId), "current-question"] as const,
};

export async function joinStudentLiveQuiz(input: StudentLiveQuizJoinRequest): Promise<StudentLiveQuizJoinResponse> {
  const response = await apiClient.post<ApiResponse<StudentLiveQuizJoinResponse>>(`${root}/join`, input);
  return unwrap(response.data);
}

export async function getStudentLiveQuizState(roomId: string): Promise<StudentLiveQuizState> {
  const response = await apiClient.get<ApiResponse<StudentLiveQuizState>>(`${root}/${roomId}/state`);
  return unwrap(response.data);
}

export async function getStudentLiveQuizCurrentQuestion(roomId: string): Promise<StudentLiveQuizCurrentQuestion> {
  const response = await apiClient.get<ApiResponse<StudentLiveQuizCurrentQuestion>>(`${root}/${roomId}/current-question`);
  return unwrap(response.data);
}

export async function answerStudentLiveQuizQuestion(
  roomId: string,
  input: StudentLiveQuizAnswerRequest,
): Promise<StudentLiveQuizAnswerResponse> {
  const response = await apiClient.post<ApiResponse<StudentLiveQuizAnswerResponse>>(`${root}/${roomId}/answer`, input);
  return unwrap(response.data);
}

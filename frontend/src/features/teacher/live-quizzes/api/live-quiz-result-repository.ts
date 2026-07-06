import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { TeacherLiveQuizResultDetail, TeacherLiveQuizResults } from "@/features/teacher/live-quizzes/model/live-quiz-contracts";

// Cac endpoint nay doc ket qua da chot tu result-service.
// Khong goi examruntime-service nua vi runtime chi phu trach thi truc tiep va phat event khi close.
const root = (roomId: string) => `/v1/api/result-service/teacher/live-quizzes/${roomId}/results`;

export const liveQuizResultKeys = {
  // Tach query key live quiz result khoi snapshot realtime de cache khong lan voi leaderboard dang chay.
  all: ["teacher", "live-quiz-results"] as const,
  room: (roomId: string) => [...liveQuizResultKeys.all, roomId] as const,
  list: (roomId: string) => [...liveQuizResultKeys.room(roomId), "list"] as const,
  detail: (roomId: string, resultId: string) => [...liveQuizResultKeys.room(roomId), "detail", resultId] as const,
};

export async function getTeacherLiveQuizResults(roomId: string, params: { page?: number; size?: number } = {}): Promise<TeacherLiveQuizResults> {
  // API nay chi co data sau khi giao vien dong phong va Result Service ingest xong event LiveQuizRoomClosed.
  const response = await apiClient.get<ApiResponse<TeacherLiveQuizResults>>(root(roomId), { params });
  return unwrap(response.data);
}

export async function getTeacherLiveQuizResultDetail(roomId: string, resultId: string): Promise<TeacherLiveQuizResultDetail> {
  // Detail chi danh cho giao vien: co snapshot cau hoi, dap an hoc sinh va dap an dung.
  const response = await apiClient.get<ApiResponse<TeacherLiveQuizResultDetail>>(`${root(roomId)}/${resultId}`);
  return unwrap(response.data);
}

export async function exportTeacherLiveQuizResults(roomId: string): Promise<Blob> {
  // Export tra ve file binary nen khong unwrap envelope JSON.
  const response = await apiClient.get(`${root(roomId)}/export`, { responseType: "blob" });
  return response.data as Blob;
}

import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { StudentLiveQuizFinalResult } from "@/features/student/live-quizzes/model/live-quiz-contracts";

export const studentLiveQuizResultKeys = {
  // Key rieng cho ket qua chinh thuc, khong dung chung voi state dang lam bai.
  // State co diem/rank tam thoi; result nay la ban da chot sau khi teacher close.
  all: ["student", "live-quiz-results"] as const,
  result: (roomId: string) => [...studentLiveQuizResultKeys.all, roomId, "result"] as const,
};

export async function getStudentLiveQuizFinalResult(roomId: string): Promise<StudentLiveQuizFinalResult> {
  // Student endpoint chi tra summary cua chinh hoc sinh, khong tra answer key/detail tung cau.
  const response = await apiClient.get<ApiResponse<StudentLiveQuizFinalResult>>(`/v1/api/result-service/student/live-quizzes/${roomId}/result`);
  return unwrap(response.data);
}

import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { StudentLiveQuizResultSummary } from "@/features/student/live-quizzes/model/live-quiz-contracts";

export const studentLiveQuizResultIndexKeys = {
  all: ["student", "live-quiz-result-index"] as const,
  list: (subjectId?: string) => [...studentLiveQuizResultIndexKeys.all, "list", subjectId ?? "all"] as const,
};

export async function listStudentLiveQuizResults(subjectId?: string): Promise<StudentLiveQuizResultSummary[]> {
  // Endpoint nay chi tra summary cua chinh hoc sinh, khong tra answer detail/correct answers.
  const response = await apiClient.get<ApiResponse<StudentLiveQuizResultSummary[]>>(
    "/v1/api/result-service/student/live-quiz-results",
    { params: subjectId ? { subjectId } : undefined },
  );
  return unwrap(response.data);
}

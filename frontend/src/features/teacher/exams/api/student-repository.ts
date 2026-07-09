import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { StudentSummary } from "@/features/teacher/exams/model/exam-contracts";

export interface StudentSearchParams {
  keyword?: string;
  page?: number;
  size?: number;
  sort?: string;
}

const studentRoot = "/v1/api/auth-service/teacher/students";

export const studentKeys = {
  all: ["teacher", "students"] as const,
  search: (params: StudentSearchParams = {}) =>
    [...studentKeys.all, "search", params] as const,
};

export async function searchStudents(
  params: StudentSearchParams = {},
): Promise<PageResponse<StudentSummary>> {
  const response = await apiClient.get<ApiResponse<PageResponse<StudentSummary>>>(studentRoot, {
    params,
  });
  return unwrap(response.data);
}

import type { ApiResponse, PageResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  Assignment,
  AssignmentMutationResult,
  AssignmentRequest,
} from "@/features/teacher/exams/model/exam-contracts";

export interface AssignmentListParams {
  page?: number;
  size?: number;
}

const root = (subjectId: string, examId: string) =>
  `/v1/api/exam-service/teacher/subjects/${subjectId}/exams/${examId}/assignments`;

export const assignmentKeys = {
  all: ["teacher", "subjects"] as const,
  list: (subjectId: string, examId: string) =>
    [
      ...assignmentKeys.all,
      subjectId,
      "exams",
      "detail",
      examId,
      "assignments",
    ] as const,
};

export async function listAssignments(
  subjectId: string,
  examId: string,
  params: AssignmentListParams = {},
): Promise<PageResponse<Assignment>> {
  const response = await apiClient.get<ApiResponse<PageResponse<Assignment>>>(
    root(subjectId, examId),
    { params },
  );
  return unwrap(response.data);
}

export async function addAssignments(
  subjectId: string,
  examId: string,
  studentIds: string[],
): Promise<AssignmentMutationResult> {
  const request: AssignmentRequest = { studentIds };
  const response = await apiClient.post<ApiResponse<AssignmentMutationResult>>(
    root(subjectId, examId),
    request,
  );
  return unwrap(response.data);
}

export async function removeAssignment(
  subjectId: string,
  examId: string,
  studentId: string,
): Promise<AssignmentMutationResult> {
  const response = await apiClient.delete<ApiResponse<AssignmentMutationResult>>(
    `${root(subjectId, examId)}/${studentId}`,
  );
  return unwrap(response.data);
}

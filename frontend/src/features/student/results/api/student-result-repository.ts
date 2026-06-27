import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { StudentResultDetail, StudentResultSummary } from "@/features/student/results/model/student-result-contracts";

export const studentResultRepository = {
  // Lay danh sach ket qua cua hoc sinh hien tai
  async listResults(): Promise<StudentResultSummary[]> {
    const response = await apiClient.get<ApiResponse<StudentResultSummary[]>>("/v1/api/result-service/student/results");
    return unwrap(response.data);
  },

  // Lay chi tiet diem tong hop cua mot ca thi
  async getResult(examId: string): Promise<StudentResultDetail> {
    const response = await apiClient.get<ApiResponse<StudentResultDetail>>(`/v1/api/result-service/student/exams/${examId}/result`);
    return unwrap(response.data);
  },
};

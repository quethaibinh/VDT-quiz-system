import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { StudentExamDetail, StudentExamPage } from "../model/student-exam-contracts";

// Repository goi cac API lien quan den thong tin ca thi cua hoc sinh tu exam-service
export const studentExamRepository = {
  // Lay danh sach ca thi duoc gan cho hoc sinh
  async listStudentExams(params: { status?: string; page?: number; size?: number }): Promise<StudentExamPage> {
    const response = await apiClient.get("/v1/api/exam-service/student/exams", { params });
    return unwrap(response.data);
  },

  // Lay thong tin chi tiet ca thi
  async getStudentExam(examId: string): Promise<StudentExamDetail> {
    const response = await apiClient.get(`/v1/api/exam-service/student/exams/${examId}`);
    return unwrap(response.data);
  },
};

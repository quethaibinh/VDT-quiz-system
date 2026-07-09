import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type {
  AutosaveRequest,
  AutosaveResponse,
  StudentJoinResponse,
  StudentPaperResponse,
  SubmitRequest,
  SubmitResponse,
} from "../model/student-exam-contracts";

// Repository ket noi den examruntime-service de xu ly thi thoi gian thuc (runtime)
export const studentRuntimeRepository = {
  // Hoc sinh vao ca thi de kiem tra tinh trang san sang va nhan sessionId
  async joinStudentExam(examId: string): Promise<StudentJoinResponse> {
    const response = await apiClient.post(`/v1/api/examruntime-service/student/exams/${examId}/join`);
    return unwrap(response.data);
  },

  // Bat dau lam bai thi (sinh de ca nhan va chuyen sang IN_PROGRESS)
  async startStudentExam(examId: string): Promise<StudentPaperResponse> {
    const response = await apiClient.post(`/v1/api/examruntime-service/student/exams/${examId}/start`);
    return unwrap(response.data);
  },

  // Lay lai de thi va cac dap an da luu truoc do (khi reload hoac doi thiet bi)
  async resumeStudentSession(sessionId: string): Promise<StudentPaperResponse> {
    const response = await apiClient.get(`/v1/api/examruntime-service/student/sessions/${sessionId}`);
    return unwrap(response.data);
  },

  // Cap nhat tu dong dap an cua hoc sinh
  async autosaveStudentAnswers(sessionId: string, request: AutosaveRequest): Promise<AutosaveResponse> {
    const response = await apiClient.put(
      `/v1/api/examruntime-service/student/sessions/${sessionId}/answers`,
      request
    );
    return unwrap(response.data);
  },

  // Nop bai thi, backend dam bao idempotent theo idempotencyKey cua client
  async submitStudentSession(sessionId: string, request: SubmitRequest): Promise<SubmitResponse> {
    const response = await apiClient.post(
      `/v1/api/examruntime-service/student/sessions/${sessionId}/submit`,
      request
    );
    return unwrap(response.data);
  },
};

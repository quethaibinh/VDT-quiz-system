import { apiClient } from "@/lib/http/api-client";
import type { ExamResults } from "@/features/teacher/results/model/result-contracts";
export async function getResults(examId: string) { return (await apiClient.get<ExamResults>(`/v1/api/result-service/teacher/exams/${examId}/results`)).data; }

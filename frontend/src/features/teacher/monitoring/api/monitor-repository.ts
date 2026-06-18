import { apiClient } from "@/lib/http/api-client";
import type { MonitorSnapshot } from "@/features/teacher/monitoring/model/monitor-contracts";
export async function getMonitor(examId: string) {
  return (await apiClient.get<MonitorSnapshot>(`/v1/api/exam-runtime-service/teacher/exams/${examId}/monitor`)).data;
}

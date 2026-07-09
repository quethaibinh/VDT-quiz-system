import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { MonitorSnapshot } from "@/features/teacher/monitoring/model/monitor-contracts";

export async function getMonitor(examId: string) {
  const response = await apiClient.get<MonitorSnapshot>(`/v1/api/examruntime-service/teacher/exams/${examId}/monitor`);
  return unwrap(response.data);
}

import type { ProctoringEventType, StudentAlert } from "@/features/teacher/monitoring/model/monitor-contracts";

export type StudentProctoringEventType = ProctoringEventType;

export interface ProctoringEventRequest {
  clientEventId: string;
  eventType: StudentProctoringEventType;
  occurredAt: string;
  metadata: string;
  durationMs?: number;
}

export type { StudentAlert };

export interface ParticipantState {
  id: string;
  name: string;
  status: "ONLINE" | "OFFLINE" | "SUBMITTED";
  answered: number;
  total: number;
  violations: number;
  risk: "LOW" | "MEDIUM" | "HIGH";
  lastSeen: string;
}
export interface MonitorSnapshot {
  examId: string;
  participants: ParticipantState[];
  events: { id: string; studentName: string; type: string; occurredAt: string }[];
}

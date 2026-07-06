export type MonitorSessionStatus =
  | "NOT_JOIN"
  | "CREATED"
  | "IN_PROGRESS"
  | "ONLINE"
  | "OFFLINE"
  | "SUBMITTED"
  | "AUTO_SUBMITTED"
  | "EXPIRED"
  | "LOCKED"
  | "UNKNOWN";

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type ProctoringSeverity = "INFO" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type ProctoringEventType =
  | "ONLINE"
  | "OFFLINE"
  | "RETURNED"
  | "TAB_HIDDEN"
  | "WINDOW_BLUR"
  | "FULLSCREEN_EXIT"
  | "COPY_ATTEMPT"
  | "PASTE_ATTEMPT"
  | "CONTEXT_MENU_OPENED"
  | "MULTI_INSTANCE_DETECTED"
  | "IP_CHANGED"
  | "USER_AGENT_CHANGED"
  | "CLIENT_TIME_DRIFT"
  | "ANSWER_BURST_SUSPECTED"
  | "LOCKED";

export interface MonitorParticipant {
  sessionId: string | null;
  studentId: string;
  studentCode: string | null;
  studentName: string;
  status: MonitorSessionStatus;
  answeredCount: number;
  totalQuestions: number;
  totalViolationCount: number;
  riskScore: number;
  riskLevel: RiskLevel;
  locked: boolean;
  lastSeenAt: string | null;
  lastHeartbeatAt: string | null;
  lastEventAt: string | null;
}

export interface MonitorEvent {
  id: string;
  examId: string;
  sessionId: string;
  studentId: string;
  eventType: ProctoringEventType;
  severity: ProctoringSeverity;
  occurredAt: string;
  receivedAt: string;
  metadata: string | null;
  countInSession: number | null;
}

export interface StudentAlert {
  examId: string;
  sessionId: string;
  type: string;
  message: string;
  occurredAt: string;
}

export interface MonitorRealtimeMessage {
  messageType: string;
  examId: string;
  sessionId: string | null;
  studentId: string | null;
  participant: MonitorParticipant | null;
  event: MonitorEvent | null;
  alert: StudentAlert | null;
  instanceId: string | null;
  occurredAt: string;
}

export interface MonitorSnapshot {
  examId: string;
  serverTime: string;
  participants: MonitorParticipant[];
  events: MonitorEvent[];
}

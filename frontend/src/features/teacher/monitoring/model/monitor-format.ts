import type {
  MonitorEvent,
  MonitorParticipant,
  MonitorSessionStatus,
  ProctoringEventType,
  ProctoringSeverity,
  RiskLevel,
} from "./monitor-contracts";

type Tone = "neutral" | "success" | "warning" | "danger";

const statusLabels: Record<MonitorSessionStatus, string> = {
  NOT_JOIN: "Chưa tham gia",
  CREATED: "Chưa bắt đầu",
  IN_PROGRESS: "Đang làm",
  ONLINE: "Online",
  OFFLINE: "Mất kết nối",
  SUBMITTED: "Đã nộp",
  AUTO_SUBMITTED: "Tự động nộp",
  EXPIRED: "Hết hạn",
  LOCKED: "Bị khóa",
  UNKNOWN: "Không rõ",
};

const eventLabels: Record<ProctoringEventType, string> = {
  ONLINE: "Kết nối",
  OFFLINE: "Mất kết nối",
  RETURNED: "Quay lại bài thi",
  TAB_HIDDEN: "Rời tab thi",
  WINDOW_BLUR: "Mất focus cửa sổ",
  FULLSCREEN_EXIT: "Thoát toàn màn hình",
  COPY_ATTEMPT: "Cố gắng sao chép",
  PASTE_ATTEMPT: "Cố gắng dán nội dung",
  CONTEXT_MENU_OPENED: "Mở menu chuột phải",
  MULTI_INSTANCE_DETECTED: "Mở nhiều phiên",
  IP_CHANGED: "Thay đổi IP",
  USER_AGENT_CHANGED: "Thay đổi trình duyệt",
  CLIENT_TIME_DRIFT: "Lệch giờ thiết bị",
  ANSWER_BURST_SUSPECTED: "Trả lời bất thường",
  LOCKED: "Khóa bài thi",
};

export function getStatusLabel(status: MonitorSessionStatus) {
  return statusLabels[status] ?? status;
}

export function getStatusTone(status: MonitorSessionStatus): Tone {
  if (status === "ONLINE" || status === "SUBMITTED" || status === "AUTO_SUBMITTED") return "success";
  if (status === "OFFLINE" || status === "EXPIRED" || status === "LOCKED") return "danger";
  if (status === "IN_PROGRESS") return "warning";
  return "neutral";
}

export function getRiskLabel(risk: RiskLevel) {
  if (risk === "LOW") return "Rủi ro thấp";
  if (risk === "MEDIUM") return "Rủi ro vừa";
  if (risk === "HIGH") return "Rủi ro cao";
  return "Rủi ro rất cao";
}

export function getRiskTone(risk: RiskLevel): Tone {
  if (risk === "LOW") return "success";
  if (risk === "MEDIUM") return "warning";
  return "danger";
}

export function getSeverityTone(severity: ProctoringSeverity): Tone {
  if (severity === "INFO" || severity === "LOW") return "neutral";
  if (severity === "MEDIUM") return "warning";
  return "danger";
}

export function getEventLabel(eventType: ProctoringEventType) {
  return eventLabels[eventType] ?? eventType;
}

export function isWarningParticipant(participant: MonitorParticipant) {
  return participant.locked
    || participant.status === "LOCKED"
    || participant.status === "OFFLINE"
    || participant.riskLevel === "HIGH"
    || participant.riskLevel === "CRITICAL"
    || participant.totalViolationCount > 0;
}

export function formatProgress(participant: MonitorParticipant) {
  return `${participant.answeredCount}/${participant.totalQuestions} câu`;
}

export function formatEventSummary(event: MonitorEvent) {
  const count = event.countInSession ? ` lần ${event.countInSession}` : "";
  return `${getEventLabel(event.eventType)}${count}`;
}

export function formatRelativeTime(value?: string | null, now = Date.now()) {
  if (!value) return "Chưa có";
  const timestamp = new Date(value).getTime();
  if (Number.isNaN(timestamp)) return "Không rõ";
  const diffSeconds = Math.max(0, Math.floor((now - timestamp) / 1000));
  if (diffSeconds < 10) return "vừa xong";
  if (diffSeconds < 60) return `${diffSeconds} giây trước`;
  const diffMinutes = Math.floor(diffSeconds / 60);
  if (diffMinutes < 60) return `${diffMinutes} phút trước`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} giờ trước`;
  return new Date(value).toLocaleString("vi-VN");
}

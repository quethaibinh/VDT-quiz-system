import type { MonitorParticipant, MonitorRealtimeMessage, MonitorSnapshot } from "./monitor-contracts";
import { isWarningParticipant } from "./monitor-format";

export type MonitorFilter = "ALL" | "WARNING" | "NOT_JOIN" | "OFFLINE" | "SUBMITTED" | "LOCKED";

const riskRank: Record<string, number> = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
};

function latestTime(participant: MonitorParticipant) {
  return Math.max(
    dateValue(participant.lastEventAt),
    dateValue(participant.lastHeartbeatAt),
    dateValue(participant.lastSeenAt),
  );
}

function dateValue(value?: string | null) {
  if (!value) return 0;
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? 0 : parsed;
}

export function sortMonitorParticipants(participants: MonitorParticipant[]) {
  return [...participants].sort((a, b) => {
    const lockDiff = Number(b.locked || b.status === "LOCKED") - Number(a.locked || a.status === "LOCKED");
    if (lockDiff) return lockDiff;
    const riskDiff = (riskRank[b.riskLevel] ?? 0) - (riskRank[a.riskLevel] ?? 0);
    if (riskDiff) return riskDiff;
    const offlineDiff = Number(b.status === "OFFLINE") - Number(a.status === "OFFLINE");
    if (offlineDiff) return offlineDiff;
    const timeDiff = latestTime(b) - latestTime(a);
    if (timeDiff) return timeDiff;
    return a.studentName.localeCompare(b.studentName, "vi");
  });
}

export function filterMonitorParticipants(
  participants: MonitorParticipant[],
  filter: MonitorFilter,
  keyword: string,
) {
  const normalized = keyword.trim().toLowerCase();
  return sortMonitorParticipants(participants.filter((participant) => {
    const matchesKeyword = !normalized
      || participant.studentName.toLowerCase().includes(normalized)
      || (participant.studentCode?.toLowerCase().includes(normalized) ?? false)
      || participant.studentId.toLowerCase().includes(normalized);
    if (!matchesKeyword) return false;
    if (filter === "WARNING") return isWarningParticipant(participant);
    if (filter === "NOT_JOIN") return participant.status === "NOT_JOIN";
    if (filter === "OFFLINE") return participant.status === "OFFLINE";
    if (filter === "SUBMITTED") return participant.status === "SUBMITTED" || participant.status === "AUTO_SUBMITTED";
    if (filter === "LOCKED") return participant.locked || participant.status === "LOCKED";
    return true;
  }));
}

export function getMonitorMetrics(participants: MonitorParticipant[]) {
  return {
    total: participants.length,
    notJoined: participants.filter((item) => item.status === "NOT_JOIN").length,
    online: participants.filter((item) => item.status === "ONLINE").length,
    warning: participants.filter(isWarningParticipant).length,
    submitted: participants.filter((item) => item.status === "SUBMITTED" || item.status === "AUTO_SUBMITTED").length,
    locked: participants.filter((item) => item.locked || item.status === "LOCKED").length,
  };
}

export function applyMonitorRealtimeMessage(snapshot: MonitorSnapshot, message: MonitorRealtimeMessage): MonitorSnapshot {
  const participants = message.participant
    ? upsertParticipant(snapshot.participants, message.participant)
    : snapshot.participants;
  const events = message.event
    ? prependEvent(snapshot.events, message.event)
    : snapshot.events;
  return {
    ...snapshot,
    participants,
    events,
    serverTime: message.occurredAt || snapshot.serverTime,
  };
}

function upsertParticipant(participants: MonitorParticipant[], next: MonitorParticipant) {
  const index = participants.findIndex((item) =>
    (next.sessionId && item.sessionId === next.sessionId)
    || (!item.sessionId && item.studentId === next.studentId)
    || (item.studentId === next.studentId && item.status === "NOT_JOIN")
  );
  if (index < 0) return sortMonitorParticipants([...participants, next]);
  const copy = [...participants];
  copy[index] = { ...copy[index], ...next };
  return sortMonitorParticipants(copy);
}

function prependEvent(events: MonitorSnapshot["events"], event: MonitorSnapshot["events"][number]) {
  const deduped = events.filter((item) => item.id !== event.id);
  return [event, ...deduped].slice(0, 100);
}

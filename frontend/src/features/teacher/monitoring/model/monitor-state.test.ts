import { describe, expect, it } from "vitest";
import type { MonitorParticipant, MonitorSnapshot } from "./monitor-contracts";
import { applyMonitorRealtimeMessage, filterMonitorParticipants, getMonitorMetrics } from "./monitor-state";
import { reconcileSnapshot } from "../hooks/use-monitor-realtime";

describe("monitor state helpers", () => {
  it("filters warning participants and prioritizes locked/high risk first", () => {
    const participants = [
      participant("s1", "An", "ONLINE", "LOW", false, 0),
      participant("s2", "Binh", "OFFLINE", "HIGH", false, 2),
      participant("s3", "Chi", "LOCKED", "MEDIUM", true, 3),
    ];

    const filtered = filterMonitorParticipants(participants, "WARNING", "");

    expect(filtered.map((item) => item.sessionId)).toEqual(["s3", "s2"]);
    expect(getMonitorMetrics(participants)).toMatchObject({
      total: 3,
      notJoined: 0,
      online: 1,
      warning: 2,
      locked: 1,
    });
  });

  it("upserts realtime participant and prepends deduplicated events", () => {
    const snapshot: MonitorSnapshot = {
      examId: "exam-1",
      serverTime: "2026-01-01T00:00:00Z",
      participants: [participant("s1", "An", "ONLINE", "LOW", false, 0)],
      events: [event("event-1", "TAB_HIDDEN")],
    };

    const next = applyMonitorRealtimeMessage(snapshot, {
      messageType: "PROCTORING_EVENT",
      examId: "exam-1",
      sessionId: "s1",
      studentId: "student-s1",
      participant: participant("s1", "An", "OFFLINE", "HIGH", false, 2),
      event: event("event-2", "FULLSCREEN_EXIT"),
      alert: null,
      instanceId: "instance",
      occurredAt: "2026-01-01T00:01:00Z",
    });

    expect(next.participants[0]).toMatchObject({ sessionId: "s1", status: "OFFLINE", totalViolationCount: 2 });
    expect(next.events.map((item) => item.id)).toEqual(["event-2", "event-1"]);
  });

  it("updates progress from participant-only realtime messages", () => {
    const snapshot: MonitorSnapshot = {
      examId: "exam-1",
      serverTime: "2026-01-01T00:00:00Z",
      participants: [participant("s1", "An", "ONLINE", "LOW", false, 0)],
      events: [event("event-1", "TAB_HIDDEN")],
    };
    const progress = participant("s1", "An", "ONLINE", "LOW", false, 0);
    progress.answeredCount = 4;

    const next = applyMonitorRealtimeMessage(snapshot, {
      messageType: "PROGRESS",
      examId: "exam-1",
      sessionId: "s1",
      studentId: "student-s1",
      participant: progress,
      event: null,
      alert: null,
      instanceId: "instance",
      occurredAt: "2026-01-01T00:02:00Z",
    });

    expect(next.participants[0]).toMatchObject({ sessionId: "s1", answeredCount: 4 });
    expect(next.events.map((item) => item.id)).toEqual(["event-1"]);
  });

  it("searches by student code and filters not-joined participants", () => {
    const participants = [
      participant(null, "Lan", "NOT_JOIN", "LOW", false, 0, "SV002"),
      participant("s1", "An", "ONLINE", "LOW", false, 0, "SV001"),
    ];

    expect(filterMonitorParticipants(participants, "ALL", "SV002").map((item) => item.studentName)).toEqual(["Lan"]);
    expect(filterMonitorParticipants(participants, "NOT_JOIN", "").map((item) => item.studentCode)).toEqual(["SV002"]);
    expect(getMonitorMetrics(participants)).toMatchObject({ total: 2, notJoined: 1, online: 1 });
  });

  it("replaces a not-joined roster row when realtime session state arrives", () => {
    const snapshot: MonitorSnapshot = {
      examId: "exam-1",
      serverTime: "2026-01-01T00:00:00Z",
      participants: [participant(null, "Lan", "NOT_JOIN", "LOW", false, 0, "SV002")],
      events: [],
    };
    const joined = participant("s2", "Lan", "ONLINE", "LOW", false, 0, "SV002");
    joined.studentId = snapshot.participants[0].studentId;

    const next = applyMonitorRealtimeMessage(snapshot, {
      messageType: "ONLINE",
      examId: "exam-1",
      sessionId: "s2",
      studentId: joined.studentId,
      participant: joined,
      event: null,
      alert: null,
      instanceId: "instance",
      occurredAt: "2026-01-01T00:03:00Z",
    });

    expect(next.participants).toHaveLength(1);
    expect(next.participants[0]).toMatchObject({ sessionId: "s2", studentCode: "SV002", status: "ONLINE" });
  });

  it("reconciles snapshots by keeping the newest participant states and merging events", () => {
    const current: MonitorSnapshot = {
      examId: "exam-1",
      serverTime: "2026-01-01T00:00:00Z",
      participants: [
        {
          ...participant("s1", "An", "ONLINE", "LOW", false, 0),
          lastSeenAt: "2026-01-01T00:05:00Z",
          totalViolationCount: 1,
        },
        {
          ...participant("s2", "Binh", "ONLINE", "LOW", false, 0),
          lastSeenAt: "2026-01-01T00:01:00Z",
        }
      ],
      events: [event("event-1", "TAB_HIDDEN")],
    };

    const next: MonitorSnapshot = {
      examId: "exam-1",
      serverTime: "2026-01-01T00:06:00Z",
      participants: [
        {
          ...participant("s1", "An", "ONLINE", "LOW", false, 0),
          lastSeenAt: "2026-01-01T00:02:00Z",
          totalViolationCount: 0,
        },
        {
          ...participant("s2", "Binh", "ONLINE", "LOW", false, 0),
          lastSeenAt: "2026-01-01T00:03:00Z",
          totalViolationCount: 2,
        }
      ],
      events: [event("event-2", "FULLSCREEN_EXIT")],
    };

    const result = reconcileSnapshot(current, next);

    const resS1 = result.participants.find(p => p.sessionId === "s1");
    expect(resS1?.totalViolationCount).toBe(1);

    const resS2 = result.participants.find(p => p.sessionId === "s2");
    expect(resS2?.totalViolationCount).toBe(2);

    expect(result.events.map(e => e.id)).toContain("event-1");
    expect(result.events.map(e => e.id)).toContain("event-2");
  });
});

function participant(
  sessionId: string | null,
  studentName: string,
  status: MonitorParticipant["status"],
  riskLevel: MonitorParticipant["riskLevel"],
  locked: boolean,
  violations: number,
  studentCode = `SV-${sessionId ?? "not-join"}`,
): MonitorParticipant {
  return {
    sessionId,
    studentId: `student-${sessionId ?? studentCode}`,
    studentCode,
    studentName,
    status,
    answeredCount: 1,
    totalQuestions: 10,
    totalViolationCount: violations,
    riskScore: violations,
    riskLevel,
    locked,
    lastSeenAt: "2026-01-01T00:00:00Z",
    lastHeartbeatAt: "2026-01-01T00:00:00Z",
    lastEventAt: violations ? "2026-01-01T00:00:01Z" : null,
  };
}

function event(id: string, eventType: "TAB_HIDDEN" | "FULLSCREEN_EXIT") {
  return {
    id,
    examId: "exam-1",
    sessionId: "s1",
    studentId: "student-s1",
    eventType,
    severity: "HIGH" as const,
    occurredAt: "2026-01-01T00:00:01Z",
    receivedAt: "2026-01-01T00:00:02Z",
    metadata: "{}",
    countInSession: 1,
  };
}

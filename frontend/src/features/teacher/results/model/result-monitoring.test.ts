import { describe, expect, it } from "vitest";
import type { MonitorEvent, MonitorParticipant } from "@/features/teacher/monitoring/model/monitor-contracts";
import type { TeacherResultRow } from "@/features/teacher/results/model/result-contracts";
import { buildGradebookView, getStudentMonitorEvents } from "./result-monitoring";

const baseParticipant: MonitorParticipant = {
  sessionId: null,
  studentId: "student-1",
  studentCode: "SV0001",
  studentName: "Nguyen Minh Anh",
  status: "NOT_JOIN",
  answeredCount: 0,
  totalQuestions: 30,
  totalViolationCount: 0,
  riskScore: 0,
  riskLevel: "LOW",
  locked: false,
  lastSeenAt: null,
  lastHeartbeatAt: null,
  lastEventAt: null,
};

const baseResult: TeacherResultRow = {
  resultId: "result-1",
  examId: "exam-1",
  studentId: "student-3",
  studentCode: "SV0003",
  studentName: "Le Van Cuong",
  originalScore: 8,
  adjustedScore: null,
  effectiveScore: 8,
  maxScore: 10,
  percentage: 80,
  rank: 1,
  totalQuestions: 30,
  correctCount: 24,
  wrongCount: 3,
  blankCount: 3,
  reviewStatus: "PENDING_REVIEW",
  visibilityState: "READY",
  submittedAt: "2026-06-30T06:28:03+07:00",
  gradedAt: "2026-06-30T06:29:03+07:00",
  releasedAt: null,
};

const baseEvent: MonitorEvent = {
  id: "event-1",
  examId: "exam-1",
  sessionId: "session-1",
  studentId: "student-1",
  eventType: "TAB_HIDDEN",
  severity: "HIGH",
  occurredAt: "2026-06-30T08:00:00+07:00",
  receivedAt: "2026-06-30T08:00:01+07:00",
  metadata: null,
  countInSession: 1,
};

describe("result monitoring gradebook helpers", () => {
  it("adds not-joined and locked students into the same gradebook with zero score", () => {
    const notJoined = baseParticipant;
    const locked: MonitorParticipant = {
      ...baseParticipant,
      studentId: "student-2",
      studentCode: "SV0002",
      studentName: "Tran Thu Ha",
      status: "LOCKED",
      locked: true,
    };

    const gradebook = buildGradebookView([baseResult], [notJoined, locked]);

    expect(gradebook.rows.map((row) => row.studentId)).toEqual(["student-3", "student-1", "student-2"]);
    expect(gradebook.participantCount).toBe(3);
    expect(gradebook.gradedCount).toBe(1);
    expect(gradebook.average).toBeCloseTo(8 / 3);
    expect(gradebook.distribution[0].count).toBe(2);
    expect(gradebook.distribution[4].count).toBe(1);
    expect(gradebook.rows.find((row) => row.studentId === "student-1")).toMatchObject({
      resultId: null,
      effectiveScore: 0,
      maxScore: 10,
      outcome: "NOT_JOIN",
      canOpenDetail: false,
    });
    expect(gradebook.rows.find((row) => row.studentId === "student-2")).toMatchObject({
      resultId: null,
      effectiveScore: 0,
      outcome: "LOCKED",
      canPublish: false,
    });
  });

  it("forces a locked student with an existing result to zero score", () => {
    const lockedWithResult: MonitorParticipant = {
      ...baseParticipant,
      studentId: "student-3",
      status: "LOCKED",
      locked: true,
    };

    const gradebook = buildGradebookView([baseResult], [lockedWithResult]);

    expect(gradebook.rows).toHaveLength(1);
    expect(gradebook.rows[0]).toMatchObject({
      resultId: "result-1",
      effectiveScore: 0,
      percentage: 0,
      outcome: "LOCKED",
      canOpenDetail: false,
    });
    expect(gradebook.distribution[0].count).toBe(1);
  });

  it("does not synthesize offline students because their autosave score comes from result rows", () => {
    const offline: MonitorParticipant = {
      ...baseParticipant,
      studentId: "student-4",
      status: "OFFLINE",
      locked: false,
    };

    const gradebook = buildGradebookView([baseResult], [offline]);

    expect(gradebook.rows.map((row) => row.studentId)).toEqual(["student-3"]);
  });

  it("filters monitor events for one student", () => {
    const otherEvent = { ...baseEvent, id: "event-2", studentId: "student-2" };

    expect(getStudentMonitorEvents([baseEvent, otherEvent], "student-1")).toEqual([baseEvent]);
  });
});

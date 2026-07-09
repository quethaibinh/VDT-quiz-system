import type { MonitorEvent, MonitorParticipant } from "@/features/teacher/monitoring/model/monitor-contracts";
import type { TeacherExamResults, TeacherResultRow } from "@/features/teacher/results/model/result-contracts";

export type GradebookOutcome = "GRADED" | "LOCKED" | "NOT_JOIN";

export interface GradebookResultRow extends Omit<TeacherResultRow, "resultId" | "reviewStatus" | "visibilityState" | "submittedAt" | "gradedAt"> {
  resultId: string | null;
  reviewStatus: TeacherResultRow["reviewStatus"] | "NOT_AVAILABLE";
  visibilityState: TeacherResultRow["visibilityState"] | "NOT_JOINED" | "LOCKED";
  submittedAt: string | null;
  gradedAt: string | null;
  outcome: GradebookOutcome;
  source: "RESULT" | "MONITOR";
  monitorStatus: string | null;
  canOpenDetail: boolean;
  canAdjust: boolean;
  canPublish: boolean;
}

export interface GradebookViewModel {
  rows: GradebookResultRow[];
  distribution: TeacherExamResults["distribution"];
  participantCount: number;
  gradedCount: number;
  releasedCount: number;
  pendingReviewCount: number;
  average: number;
  highest: number;
  lowest: number;
}

const zeroDistribution: TeacherExamResults["distribution"] = [
  { range: "0-20%", count: 0 },
  { range: "21-40%", count: 0 },
  { range: "41-60%", count: 0 },
  { range: "61-80%", count: 0 },
  { range: "81-100%", count: 0 },
];

export function buildGradebookView(
  results: TeacherResultRow[],
  participants: MonitorParticipant[],
  fallbackDistribution: TeacherExamResults["distribution"] = zeroDistribution,
): GradebookViewModel {
  const participantByStudent = new Map(participants.map((participant) => [participant.studentId, participant]));
  const resultStudentIds = new Set(results.map((row) => row.studentId));
  const fallbackMaxScore = findFallbackMaxScore(results);
  const rows: GradebookResultRow[] = [
    ...results.map((row) => {
      const participant = participantByStudent.get(row.studentId);
      const locked = isLockedParticipant(participant);
      return {
        ...row,
        originalScore: locked ? 0 : row.originalScore,
        adjustedScore: locked ? null : row.adjustedScore,
        effectiveScore: locked ? 0 : row.effectiveScore,
        percentage: locked ? 0 : row.percentage,
        correctCount: locked ? 0 : row.correctCount,
        wrongCount: locked ? 0 : row.wrongCount,
        blankCount: locked ? row.totalQuestions : row.blankCount,
        rank: 0,
        reviewStatus: locked ? "NOT_AVAILABLE" as const : row.reviewStatus,
        visibilityState: locked ? "LOCKED" as const : row.visibilityState,
        outcome: locked ? "LOCKED" as const : "GRADED" as const,
        source: "RESULT" as const,
        monitorStatus: participant?.status ?? null,
        canOpenDetail: !locked,
        canAdjust: !locked,
        canPublish: !locked && row.reviewStatus !== "RELEASED",
      };
    }),
    ...participants
      .filter((participant) => !resultStudentIds.has(participant.studentId))
      .filter((participant) => participant.status === "NOT_JOIN" || isLockedParticipant(participant))
      .map((participant) => syntheticRow(participant, fallbackMaxScore)),
  ];

  const rankedRows = rankRows(rows);
  return {
    rows: rankedRows,
    distribution: rankedRows.length ? distribution(rankedRows) : fallbackDistribution,
    participantCount: rankedRows.length,
    gradedCount: rankedRows.filter((row) => row.source === "RESULT" && row.outcome === "GRADED").length,
    releasedCount: rankedRows.filter((row) => row.reviewStatus === "RELEASED").length,
    pendingReviewCount: rankedRows.filter((row) => row.reviewStatus === "PENDING_REVIEW").length,
    average: average(rankedRows),
    highest: rankedRows.length ? Math.max(...rankedRows.map((row) => row.effectiveScore)) : 0,
    lowest: rankedRows.length ? Math.min(...rankedRows.map((row) => row.effectiveScore)) : 0,
  };
}

export function getStudentMonitorEvents(events: MonitorEvent[], studentId: string) {
  return events.filter((event) => event.studentId === studentId);
}

function syntheticRow(participant: MonitorParticipant, maxScore: number): GradebookResultRow {
  const totalQuestions = participant.totalQuestions || 0;
  const locked = isLockedParticipant(participant);
  return {
    resultId: null,
    examId: "",
    studentId: participant.studentId,
    studentCode: participant.studentCode,
    studentName: participant.studentName,
    originalScore: 0,
    adjustedScore: null,
    effectiveScore: 0,
    maxScore,
    percentage: 0,
    rank: 0,
    totalQuestions,
    correctCount: 0,
    wrongCount: 0,
    blankCount: totalQuestions,
    reviewStatus: "NOT_AVAILABLE",
    visibilityState: locked ? "LOCKED" : "NOT_JOINED",
    submittedAt: null,
    gradedAt: null,
    releasedAt: null,
    outcome: locked ? "LOCKED" : "NOT_JOIN",
    source: "MONITOR",
    monitorStatus: participant.status,
    canOpenDetail: false,
    canAdjust: false,
    canPublish: false,
  };
}

function rankRows(rows: GradebookResultRow[]) {
  const sorted = [...rows].sort((a, b) => {
    const scoreDiff = b.effectiveScore - a.effectiveScore;
    if (scoreDiff) return scoreDiff;
    const submittedDiff = dateValue(a.submittedAt) - dateValue(b.submittedAt);
    if (submittedDiff) return submittedDiff;
    return a.studentName.localeCompare(b.studentName, "vi");
  });

  let previousScore: number | null = null;
  let previousRank = 0;
  return sorted.map((row, index) => {
    const rank = previousScore != null && previousScore === row.effectiveScore ? previousRank : index + 1;
    previousScore = row.effectiveScore;
    previousRank = rank;
    return { ...row, rank };
  });
}

function distribution(rows: GradebookResultRow[]): TeacherExamResults["distribution"] {
  const buckets = [0, 0, 0, 0, 0];
  for (const row of rows) {
    const percentage = row.maxScore > 0 ? (row.effectiveScore / row.maxScore) * 100 : 0;
    const index = Math.min(Math.floor(percentage / 20), 4);
    buckets[index] += 1;
  }
  return zeroDistribution.map((bucket, index) => ({ ...bucket, count: buckets[index] }));
}

function average(rows: GradebookResultRow[]) {
  if (!rows.length) return 0;
  return rows.reduce((total, row) => total + row.effectiveScore, 0) / rows.length;
}

function findFallbackMaxScore(results: TeacherResultRow[]) {
  return results.find((row) => row.maxScore > 0)?.maxScore ?? 0;
}

function isLockedParticipant(participant?: MonitorParticipant) {
  return Boolean(participant && (participant.locked || participant.status === "LOCKED"));
}

function dateValue(value?: string | null) {
  if (!value) return Number.MAX_SAFE_INTEGER;
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? Number.MAX_SAFE_INTEGER : parsed;
}

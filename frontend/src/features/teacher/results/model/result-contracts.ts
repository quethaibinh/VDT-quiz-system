import type { PageResponse } from "@/lib/api-types";

export type ResultReviewStatus = "PENDING_REVIEW" | "RELEASED";
export type ResultVisibilityState =
  | "GRADING"
  | "READY"
  | "PENDING_REVIEW"
  | "RELEASED"
  | "LOCKED_UNTIL_CLOSED"
  | "GRADING_FAILED"
  | "CONFIG_MISSING";

export interface TeacherResultRow {
  resultId: string;
  examId: string;
  studentId: string;
  studentCode: string | null;
  studentName: string;
  originalScore: number;
  adjustedScore: number | null;
  effectiveScore: number;
  maxScore: number;
  percentage: number;
  rank: number;
  totalQuestions: number;
  correctCount: number;
  wrongCount: number;
  blankCount: number;
  reviewStatus: ResultReviewStatus;
  visibilityState: ResultVisibilityState;
  submittedAt: string;
  gradedAt: string;
  releasedAt: string | null;
}

export interface TeacherExamResults {
  examId: string;
  code: string | null;
  title: string;
  subjectName: string | null;
  showResultPolicy: string | null;
  startAt: string | null;
  endAt: string | null;
  participantCount: number;
  gradedCount: number;
  releasedCount: number;
  pendingReviewCount: number;
  average: number;
  highest: number;
  lowest: number;
  distribution: { range: string; count: number }[];
  students: PageResponse<TeacherResultRow>;
}

export interface TeacherResultDetail {
  summary: TeacherResultRow;
  adjustmentReason: string | null;
  adjustedAt: string | null;
  adjustedBy: string | null;
  answers: {
    questionId: string;
    questionOrder: number;
    selectedOptionIds: string[];
    correctOptionIds: string[];
    correct: boolean;
    scoreAwarded: number;
    maxScore: number;
    gradingNote: string | null;
    questionSnapshot: unknown;
  }[];
  incidents: { type: string; message: string; occurredAt: string }[];
  gradingErrors: { type: string; message: string; occurredAt: string }[];
}

export interface PublishResultsResponse {
  requestedCount: number;
  publishedCount: number;
  unchangedCount: number;
}

export type StudentResultVisibilityState =
  | "GRADING"
  | "READY"
  | "PENDING_REVIEW"
  | "RELEASED"
  | "LOCKED_UNTIL_CLOSED"
  | "GRADING_FAILED"
  | "CONFIG_MISSING";

export interface StudentResultSummary {
  examId: string;
  code: string | null;
  title: string;
  subjectName: string | null;
  visibilityState: StudentResultVisibilityState;
  message: string;
  availableAt: string | null;
  score: number | null;
  maxScore: number | null;
  percentage: number | null;
  rank: number | null;
  gradedCount: number | null;
  submittedAt: string;
  gradedAt: string;
  releasedAt: string | null;
}

export interface StudentResultDetail extends StudentResultSummary {
  totalQuestions: number;
  correctCount: number;
  wrongCount: number;
  blankCount: number;
}

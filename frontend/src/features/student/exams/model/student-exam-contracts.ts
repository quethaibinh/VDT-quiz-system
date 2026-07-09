import type { PageResponse } from "@/lib/api-types";

// Dinh nghia cac kieu du lieu cho phan he thi cua hoc sinh

export type StudentExamAvailability = "UPCOMING" | "OPEN" | "ENDED";

export interface StudentExamSummary {
  examId: string;
  code: string;
  title: string;
  subjectId: string;
  subjectName: string;
  startAt: string;
  endAt: string;
  durationMinutes: number;
  questionCount: number;
  status: "DRAFT" | "SCHEDULED" | "ACTIVE" | "CLOSED" | "CANCELLED";
  studentAvailability: StudentExamAvailability;
  assignmentStatus: "ASSIGNED" | "REMOVED" | "BLOCKED";
}

export interface StudentExamDetail {
  examId: string;
  code: string;
  title: string;
  description: string;
  subjectId: string;
  subjectName: string;
  startAt: string;
  endAt: string;
  durationMinutes: number;
  questionCount: number;
  status: "DRAFT" | "SCHEDULED" | "ACTIVE" | "CLOSED" | "CANCELLED";
  studentAvailability: StudentExamAvailability;
  assignmentStatus: "ASSIGNED" | "REMOVED" | "BLOCKED";
}

export interface StudentExamPage extends PageResponse<StudentExamSummary> {
  serverTime: string;
}

export interface StudentJoinResponse {
  sessionId: string;
  examId: string;
  status: "CREATED" | "IN_PROGRESS" | "SUBMITTED" | "AUTO_SUBMITTED" | "EXPIRED" | "LOCKED";
  serverTime: string;
  startAt: string;
  endAt: string;
  canStart: boolean;
  remainingSecondsToStart: number;
}

export interface StudentOption {
  optionId: string;
  key: string;
  content: string;
  contentFormat: "TEXT" | "HTML";
}

export interface StudentQuestion {
  questionId: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  type: "SINGLE_CHOICE" | "MULTIPLE_CHOICE";
  content: string;
  contentFormat: "TEXT" | "HTML";
  score: number;
  options: StudentOption[];
}

export interface StudentAnswer {
  questionId: string;
  selectedOptionIds: string[];
  answerText: string | null;
  markedForReview: boolean;
}

export interface StudentPaperResponse {
  sessionId: string;
  status: "CREATED" | "IN_PROGRESS" | "SUBMITTED" | "AUTO_SUBMITTED" | "EXPIRED" | "LOCKED";
  serverStartedAt: string;
  serverDeadlineAt: string;
  questions: StudentQuestion[];
  answers: StudentAnswer[];
}

export interface AutosaveRequest {
  clientSeq: number;
  answers: StudentAnswer[];
}

export interface AutosaveResponse {
  sessionId: string;
  acceptedSeq: number;
  serverSeq: number;
  savedCount: number;
  skippedCount: number;
  storeMode: "REDIS" | "DB_FALLBACK";
  lastAutosaveAt: string;
}

export interface SubmitRequest {
  idempotencyKey: string;
  clientSeq: number;
  finalAnswers: StudentAnswer[];
}

export interface SubmitResponse {
  submissionId: string;
  sessionId: string;
  status: "RECEIVED" | "ACCEPTED" | "SUBMITTED" | "AUTO_SUBMITTED";
  submitReason: "STUDENT" | "TIME_UP" | "AUTO";
  submittedAt: string;
}

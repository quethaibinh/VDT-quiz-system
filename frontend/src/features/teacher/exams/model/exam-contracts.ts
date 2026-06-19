export type ExamStatus = "DRAFT" | "SCHEDULED" | "ACTIVE" | "CLOSED" | "CANCELLED";
export type ShowResultPolicy = "NEVER" | "AFTER_SUBMIT" | "AFTER_CLOSED";
export type HandleViolation = "LOCK" | "PAUSE" | "WARN";
export type AssignmentStatus = "ASSIGNED" | "REMOVED" | "BLOCKED";

export interface ExamSummary {
  id: string;
  code: string;
  title: string;
  subjectId: string;
  subjectName: string;
  collectionId: string;
  collectionName: string;
  startAt: string;
  durationMinutes: number;
  questionCount: number;
  assignedCount: number;
  status: ExamStatus;
  version: number;
}

export interface ExamDetail extends Omit<ExamSummary, "questionCount"> {
  description: string | null;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  endAt: string;
  joinBeforeMinutes: number;
  joinAfterMinutes: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  showResultPolicy: ShowResultPolicy;
  autoSubmit: boolean;
  requireFullscreen: boolean;
  maxViolationAllowed: number;
  handleViolation: HandleViolation;
}

export interface ExamDraftRequest {
  title: string;
  description: string | null;
  collectionId: string;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  startAt: string;
  durationMinutes: number;
  joinBeforeMinutes: number;
  joinAfterMinutes: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  showResultPolicy: ShowResultPolicy;
  autoSubmit: boolean;
  requireFullscreen: boolean;
  maxViolationAllowed: number;
  handleViolation: HandleViolation;
}

export interface StudentSummary {
  id: string;
  studentCode: string;
  fullName: string;
  displayName: string;
}

export interface Assignment {
  id: string;
  studentId: string;
  studentCode: string;
  studentName: string;
  status: AssignmentStatus;
  assignedAt: string;
  removedAt: string | null;
}

export interface AssignmentRequest {
  studentIds: string[];
}

export interface AssignmentMutationResult {
  requestedCount: number;
  assignedCount: number;
  unchangedCount: number;
  assignments: Assignment[];
}

export interface ExamWizardState extends Omit<ExamDraftRequest, "startAt"> {
  startAtLocal: string;
  selectedStudents: Record<string, StudentSummary>;
}

export type ExamDraftInput = ExamDraftRequest;

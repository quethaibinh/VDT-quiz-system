export type ExamStatus = "DRAFT" | "SCHEDULED" | "ACTIVE" | "CLOSED";

export interface ExamSummary {
  id: string;
  title: string;
  subjectId: string;
  subjectName: string;
  startAt: string;
  durationMinutes: number;
  questionCount: number;
  assignedCount: number;
  status: ExamStatus;
}

export interface ExamDraftInput {
  title: string;
  subjectId: string;
  collectionId: string;
  startAt: string;
  durationMinutes: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
}

export interface ExamResults {
  examId: string;
  participantCount: number;
  gradedCount: number;
  average: number;
  highest: number;
  lowest: number;
  distribution: { range: string; count: number }[];
  students: { id: string; name: string; score: number; correct: number; wrong: number; status: string }[];
}

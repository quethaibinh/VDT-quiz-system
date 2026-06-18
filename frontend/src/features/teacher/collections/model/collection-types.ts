export interface CollectionStats {
  questionCount: number;
  easy: number;
  medium: number;
  hard: number;
}

export interface QuestionCollection {
  id: string;
  subjectId: string;
  ownerTeacherId: string;
  name: string;
  description?: string | null;
  visibility: "PRIVATE" | "PUBLIC";
  status: "ACTIVE" | "ARCHIVED";
  editable: boolean;
  stats: CollectionStats;
  createdAt: string;
  updatedAt: string;
}

export interface CollectionInput {
  name: string;
  description?: string;
  visibility: "PRIVATE" | "PUBLIC";
}

export interface BulkCollectionResult {
  requestedCount: number;
  matchedCount: number;
  addedCount: number;
  removedCount: number;
  skippedCount: number;
  totalQuestionCount: number;
}

export interface ImportQuestionError {
  rowNumber: number;
  fieldName: string;
  errorCode: string;
  message: string;
}
export interface ImportQuestionResult {
  totalRows: number;
  successCount: number;
  failedCount: number;
  imported: boolean;
  importJobId?: string | null;
  createdTopicCount: number;
  createdQuestionCount: number;
  errors: ImportQuestionError[];
}

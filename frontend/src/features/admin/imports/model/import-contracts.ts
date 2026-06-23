export interface ImportUserError {
  rowNumber: number;
  fieldName: string;
  errorCode: string;
  message: string;
}

export interface ImportUserResult {
  totalRows: number;
  successCount: number;
  failedCount: number;
  errors: ImportUserError[];
}

import type { StudentAnswer, StudentQuestion } from "../model/student-exam-contracts";

// Khoi tao danh sach cau tra loi mac dinh tu de thi
export function initializeAnswers(questions: StudentQuestion[], savedAnswers: StudentAnswer[] = []): Record<string, StudentAnswer> {
  const answerMap: Record<string, StudentAnswer> = {};

  // Thiet lap cac cau tra loi rong lam mac dinh
  questions.forEach((question) => {
    answerMap[question.questionId] = {
      questionId: question.questionId,
      selectedOptionIds: [],
      answerText: null,
      markedForReview: false,
    };
  });

  // Ghi de bang cac cau tra loi da duoc luu tu backend (neu co)
  savedAnswers.forEach((saved) => {
    if (answerMap[saved.questionId]) {
      answerMap[saved.questionId] = {
        ...answerMap[saved.questionId],
        selectedOptionIds: saved.selectedOptionIds || [],
        answerText: saved.answerText,
        markedForReview: saved.markedForReview || false,
      };
    }
  });

  return answerMap;
}

// Chon dap an cho cau trac nghiem don lua chon (Single Choice)
export function toggleSingleChoice(optionId: string): string[] {
  return [optionId];
}

// Chon/bo chon dap an cho cau trac nghiem nhieu lua chon (Multiple Choice)
export function toggleMultipleChoice(currentSelected: string[], optionId: string): string[] {
  if (currentSelected.includes(optionId)) {
    return currentSelected.filter((id) => id !== optionId);
  }
  return [...currentSelected, optionId];
}

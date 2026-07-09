import { Bookmark, BookmarkCheck } from "lucide-react";
import type { StudentAnswer, StudentQuestion } from "../model/student-exam-contracts";
import { QuizQuestionCard } from "@/features/student/shared/components/quiz-question-card";

interface QuestionViewProps {
  question: StudentQuestion;
  index: number;
  answer: StudentAnswer;
  disabled?: boolean;
  onAnswerChange: (selectedOptionIds: string[]) => void;
  onMarkForReviewToggle: () => void;
}

// Hien thi chi tiet cau hoi hien tai va cac lua chon dap an
export function QuestionView({
  question,
  index,
  answer,
  disabled,
  onAnswerChange,
  onMarkForReviewToggle,
}: QuestionViewProps) {
  const isMultiple = question.type === "MULTIPLE_CHOICE";

  const handleOptionClick = (optionId: string) => {
    if (disabled) return;
    if (isMultiple) {
      const current = answer.selectedOptionIds;
      onAnswerChange(current.includes(optionId)
        ? current.filter((id) => id !== optionId)
        : [...current, optionId]);
      return;
    }
    onAnswerChange([optionId]);
  };

  return (
    <QuizQuestionCard
      content={question.content}
      options={question.options}
      isMultipleChoice={isMultiple}
      selectedOptionIds={answer.selectedOptionIds}
      disabled={disabled}
      onOptionSelect={handleOptionClick}
      header={
        <div className="flex flex-col gap-3 pb-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="space-y-1">
            <h2 className="m-0 text-lg font-bold text-ink">Câu {index + 1}</h2>
            <span className="inline-block rounded bg-ink/5 px-2 py-0.5 text-xs font-medium text-muted">
              Độ khó: {question.difficulty === "EASY" ? "Dễ" : question.difficulty === "MEDIUM" ? "Trung bình" : "Khó"}
            </span>
          </div>

          <button
            type="button"
            onClick={onMarkForReviewToggle}
            disabled={disabled}
            className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-semibold transition ${
              answer.markedForReview
                ? "border-warning bg-warning/5 text-warning"
                : "border-line bg-transparent text-muted hover:text-ink"
            } disabled:cursor-not-allowed`}
          >
            {answer.markedForReview ? <BookmarkCheck size={14} /> : <Bookmark size={14} />}
            {answer.markedForReview ? "Đã đánh dấu" : "Đánh dấu xem lại"}
          </button>
        </div>
      }
    />
  );
}

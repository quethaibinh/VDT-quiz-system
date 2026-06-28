import { Bookmark, BookmarkCheck } from "lucide-react";
import type { StudentAnswer, StudentQuestion } from "../model/student-exam-contracts";

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
    <section className="space-y-6 rounded-2xl border border-line bg-surface p-5 shadow-soft md:p-6">
      <div className="flex flex-col gap-3 border-b border-line pb-4 sm:flex-row sm:items-center sm:justify-between">
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

      <div className="whitespace-pre-wrap text-base leading-relaxed text-ink">
        {question.content}
      </div>

      <div className="grid gap-3">
        {question.options.map((option) => {
          const isSelected = answer.selectedOptionIds.includes(option.optionId);
          return (
            <button
              key={option.optionId}
              type="button"
              onClick={() => handleOptionClick(option.optionId)}
              disabled={disabled}
              className={`flex w-full items-start gap-3 rounded-xl border p-4 text-left transition ${
                isSelected
                  ? "border-primary bg-primary/5 font-semibold text-ink"
                  : "border-line bg-transparent text-muted hover:border-ink/30 hover:bg-ink/5"
              } disabled:cursor-not-allowed`}
            >
              <span
                className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center border ${
                  isMultiple ? "rounded-md" : "rounded-full"
                } ${isSelected ? "border-primary bg-primary text-white" : "border-line bg-surface"}`}
              >
                {isSelected && <span className="text-[10px] font-bold">{isMultiple ? "OK" : ""}</span>}
              </span>
              <span className="shrink-0 font-bold text-ink">{option.key}.</span>
              <span className="text-sm leading-relaxed">{option.content}</span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

import type { StudentAnswer, StudentQuestion } from "../model/student-exam-contracts";

interface QuestionNavigatorProps {
  questions: StudentQuestion[];
  answers: Record<string, StudentAnswer>;
  currentIndex: number;
  onSelect: (index: number) => void;
}

// Sidebar dieu huong giup di nhanh den cau hoi va xem tien do
export function QuestionNavigator({ questions, answers, currentIndex, onSelect }: QuestionNavigatorProps) {
  const stats = questions.reduce(
    (acc, question) => {
      const answer = answers[question.questionId];
      if (answer?.selectedOptionIds.length) acc.answered += 1;
      if (answer?.markedForReview) acc.marked += 1;
      return acc;
    },
    { answered: 0, marked: 0 }
  );

  const unanswered = questions.length - stats.answered;

  return (
    <section className="space-y-5 rounded-2xl border border-line bg-surface p-5 shadow-soft">
      <h3 className="font-bold text-ink">Danh sách câu hỏi</h3>

      <div className="grid grid-cols-5 gap-2.5 sm:grid-cols-8 lg:grid-cols-5">
        {questions.map((question, index) => {
          const answer = answers[question.questionId];
          const hasAnswer = Boolean(answer?.selectedOptionIds.length);
          const isMarked = Boolean(answer?.markedForReview);
          const isCurrent = index === currentIndex;

          let buttonClass = "bg-canvas border-line text-muted";
          if (hasAnswer) buttonClass = "bg-success/15 border-success text-success font-semibold";
          if (isMarked) buttonClass = "bg-warning/15 border-warning text-warning font-semibold";
          if (isCurrent) buttonClass = "bg-primary border-primary text-white font-bold ring-2 ring-primary/20";

          return (
            <button
              key={question.questionId}
              type="button"
              onClick={() => onSelect(index)}
              className={`flex aspect-square items-center justify-center rounded-lg border text-sm font-medium transition ${buttonClass}`}
            >
              {index + 1}
            </button>
          );
        })}
      </div>

      <div className="grid grid-cols-3 gap-2 border-t border-line pt-4 text-center text-xs">
        <div className="rounded-lg border border-success/10 bg-success/5 p-2">
          <span className="block text-sm font-bold text-success">{stats.answered}</span>
          <span className="text-[10px] text-muted">Đã làm</span>
        </div>
        <div className="rounded-lg border border-warning/10 bg-warning/5 p-2">
          <span className="block text-sm font-bold text-warning">{stats.marked}</span>
          <span className="text-[10px] text-muted">Đánh dấu</span>
        </div>
        <div className="rounded-lg border border-line bg-ink/5 p-2">
          <span className="block text-sm font-bold text-ink">{unanswered}</span>
          <span className="text-[10px] text-muted">Chưa làm</span>
        </div>
      </div>
    </section>
  );
}

import { Clock, Medal, Send, Sparkles } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";
import type { StudentLiveQuizSelectedOptionResult } from "@/features/student/live-quizzes/model/live-quiz-contracts";

interface LiveQuizStageOption {
  optionId: string;
  key: string;
  content: string;
}

interface LiveQuizQuestionStageProps {
  quizTitle: string;
  subjectName: string;
  questionPosition: number;
  totalQuestions: number;
  content: string;
  options: LiveQuizStageOption[];
  isMultipleChoice: boolean;
  selectedOptionIds: string[];
  optionFeedbackById?: Record<string, StudentLiveQuizSelectedOptionResult>;
  feedbackActive?: boolean;
  disabled?: boolean;
  submitting?: boolean;
  remainingSeconds: number;
  scoreText: string;
  rankText: string;
  onOptionSelect: (optionId: string) => void;
  onSubmit: () => void;
  canSubmit: boolean;
  feedback?: ReactNode;
}

export function LiveQuizQuestionStage({
  quizTitle,
  subjectName,
  questionPosition,
  totalQuestions,
  content,
  options,
  isMultipleChoice,
  selectedOptionIds,
  optionFeedbackById = {},
  feedbackActive,
  disabled,
  submitting,
  remainingSeconds,
  scoreText,
  rankText,
  onOptionSelect,
  onSubmit,
  canSubmit,
  feedback,
}: LiveQuizQuestionStageProps) {
  const progress = totalQuestions > 0 ? Math.min(100, Math.max(0, questionPosition / totalQuestions * 100)) : 0;
  const answerGridClass = getAnswerGridClass(options.length);
  const isFiveOptionLayout = options.length === 5;
  const dangerTime = remainingSeconds <= 5;

  return (
    <main className="mx-auto w-full max-w-6xl">
      <section className="overflow-hidden rounded-[22px] border border-[#f1d9d2] bg-[#fff7f3] shadow-[0_18px_54px_rgba(89,47,35,0.08)]">
        <header className="flex flex-wrap items-center justify-between gap-3 bg-white px-4 py-3 md:px-7">
          <div className="min-w-[190px] flex-1">
            <div className="flex items-center gap-3">
              <span className="text-xs font-extrabold text-primary md:text-sm">
                Câu hỏi {pad(questionPosition)}/{pad(totalQuestions)}
              </span>
              <div className="h-2 min-w-20 max-w-44 flex-1 overflow-hidden rounded-full bg-[#efd8d1]">
                <div className="h-full rounded-full bg-[#be4d42] transition-all" style={{ width: `${progress}%` }} />
              </div>
            </div>
            <p className="m-0 mt-1 truncate text-[11px] font-semibold text-muted">{quizTitle}</p>
          </div>

          <div className={cn(
            "order-first mx-auto inline-flex min-h-10 min-w-28 items-center justify-center gap-2 rounded-full border px-5 text-base font-black tabular-nums sm:order-none md:text-lg",
            dangerTime ? "border-danger/25 bg-danger/10 text-danger" : "border-[#f0d2cb] bg-[#fff2ed] text-primary",
          )}>
            <Clock size={18} />
            {formatClock(remainingSeconds)}
          </div>

          <div className="flex flex-1 items-center justify-end gap-5 text-right">
            <Metric label="Điểm" value={scoreText} />
            <Metric label="Hạng" value={rankText} icon={<Medal size={13} />} />
          </div>
        </header>

        <div className="px-4 pb-5 pt-8 md:px-8 md:pb-8 md:pt-14">
          <section className="mx-auto w-full max-w-5xl rounded-[24px] border border-[#f2dfda] bg-white px-5 py-8 text-center shadow-[0_16px_38px_rgba(72,48,42,0.06)] md:px-10 md:py-10">
            <span className="inline-flex items-center gap-1 rounded-full bg-[#f8e2de] px-3 py-1 text-[10px] font-extrabold uppercase text-primary">
              <Sparkles size={12} />
              {subjectName}
            </span>
            <h1 className="m-0 mx-auto mt-4 max-w-4xl whitespace-pre-wrap font-serif text-3xl font-semibold leading-tight text-[#31211f] md:text-5xl">
              {content}
            </h1>
            <p className="m-0 mt-3 text-[11px] font-semibold text-muted md:text-xs">
              {isMultipleChoice ? "Chọn tất cả đáp án đúng trước khi hết giờ." : "Chọn đáp án đúng trước khi hết giờ."}
            </p>
          </section>

          <section className={cn("mt-10 grid gap-4 md:gap-5", answerGridClass)}>
            {options.map((option) => {
              const selected = selectedOptionIds.includes(option.optionId);
              const feedbackResult = optionFeedbackById[option.optionId];
              return (
                <button
                  key={option.optionId}
                  type="button"
                  disabled={disabled}
                  onClick={() => onOptionSelect(option.optionId)}
                  className={cn(
                    "group border bg-white px-5 py-4 text-left shadow-[0_8px_22px_rgba(72,48,42,0.045)] transition disabled:cursor-not-allowed",
                    "rounded-[16px] hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-[0_14px_28px_rgba(72,48,42,0.08)]",
                    selected ? "border-primary bg-white ring-2 ring-primary/10" : "border-[#efd9d4]",
                    feedbackResult && optionFeedbackCardClass(feedbackResult),
                    feedbackResult && feedbackActive && "quiz-answer-flash",
                    disabled && !feedbackResult && "opacity-75",
                    isFiveOptionLayout ? "flex min-h-[136px] flex-col items-center justify-center gap-3 text-center" : "flex min-h-[86px] items-center gap-4",
                  )}
                >
                  <span className={cn(
                    "flex h-12 w-12 shrink-0 items-center justify-center rounded-full text-sm font-black transition",
                    feedbackResult ? optionFeedbackBadgeClass(feedbackResult) : selected ? "bg-primary text-white" : "bg-[#f8e2de] text-primary group-hover:bg-primary/15",
                  )}>
                    {option.key}
                  </span>
                  <span className={cn(
                    "min-w-0 font-extrabold leading-snug text-[#3c302d]",
                    feedbackResult && optionFeedbackTextClass(feedbackResult),
                    isFiveOptionLayout ? "text-center text-sm lg:text-base" : "text-sm md:text-base",
                  )}>
                    {option.content}
                  </span>
                </button>
              );
            })}
          </section>

          {feedback && <div className="mt-5">{feedback}</div>}

          <div className="mt-7 flex justify-end">
            {isMultipleChoice && (
              <Button disabled={!canSubmit} loading={submitting} onClick={onSubmit}>
                <Send size={16} />
                Trả lời
              </Button>
            )}
          </div>
        </div>
      </section>
    </main>
  );
}

function optionFeedbackCardClass(result: StudentLiveQuizSelectedOptionResult) {
  if (result === "CORRECT") return "border-success/50 bg-success/10 ring-2 ring-success/20";
  if (result === "WRONG") return "border-danger/50 bg-danger/10 ring-2 ring-danger/20";
  return "border-sky-300 bg-sky-50 ring-2 ring-sky-200/70";
}

function optionFeedbackBadgeClass(result: StudentLiveQuizSelectedOptionResult) {
  if (result === "CORRECT") return "bg-success text-white";
  if (result === "WRONG") return "bg-danger text-white";
  return "bg-sky-500 text-white";
}

function optionFeedbackTextClass(result: StudentLiveQuizSelectedOptionResult) {
  if (result === "CORRECT") return "text-success";
  if (result === "WRONG") return "text-danger";
  return "text-sky-700";
}

function Metric({ label, value, icon }: { label: string; value: string; icon?: ReactNode }) {
  return (
    <div>
      <p className="m-0 text-[10px] font-extrabold uppercase text-muted/70">{label}</p>
      <p className="m-0 inline-flex items-center justify-end gap-1 text-sm font-black text-primary">
        {icon}
        {value}
      </p>
    </div>
  );
}

function getAnswerGridClass(length: number) {
  if (length === 4) return "grid-cols-1 sm:grid-cols-2";
  if (length === 5) return "grid-cols-1 md:grid-cols-5";
  if (length === 6) return "grid-cols-1 lg:grid-cols-3";
  return "grid-cols-1 sm:grid-cols-2";
}

function pad(value: number) {
  return String(value).padStart(2, "0");
}

function formatClock(totalSeconds: number) {
  const safeSeconds = Math.max(0, totalSeconds);
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return `${pad(minutes)}:${pad(seconds)}`;
}

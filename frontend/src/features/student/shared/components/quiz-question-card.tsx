import type { ReactNode } from "react";

export interface QuizOption {
  optionId: string;
  key: string;
  content: string;
}

export interface QuizQuestionCardProps {
  content: ReactNode;
  options: QuizOption[];
  isMultipleChoice?: boolean;
  selectedOptionIds: string[];
  disabled?: boolean;
  onOptionSelect: (optionId: string) => void;
  header?: ReactNode;
}

export function QuizQuestionCard({
  content,
  options,
  isMultipleChoice,
  selectedOptionIds,
  disabled,
  onOptionSelect,
  header,
}: QuizQuestionCardProps) {
  const getGridColsClass = (length: number) => {
    if (length === 4) return "grid-cols-1 sm:grid-cols-2";
    if (length === 5) return "grid-cols-1 sm:grid-cols-2 lg:grid-cols-5";
    if (length === 6) return "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3";
    return "grid-cols-1"; // Fallback
  };

  const isFiveOptions = options.length === 5;

  return (
    <section className="space-y-6 rounded-[32px] bg-[#FEF6F0] p-6 shadow-sm md:p-10">
      {header && <div className="mb-4">{header}</div>}

      <div className="text-center">
        <h2 className="mx-auto max-w-4xl whitespace-pre-wrap font-serif text-3xl font-bold leading-tight text-ink md:text-4xl">
          {content}
        </h2>
        <p className="mt-4 text-sm font-medium text-muted/70">
          {isMultipleChoice
            ? "Chọn tất cả đáp án đúng bên dưới trước khi hết giờ."
            : "Chọn đáp án đúng bên dưới trước khi hết giờ."}
        </p>
      </div>

      <div className={`mt-10 grid gap-4 ${getGridColsClass(options.length)}`}>
        {options.map((option) => {
          const isSelected = selectedOptionIds.includes(option.optionId);
          return (
            <button
              key={option.optionId}
              type="button"
              onClick={() => onOptionSelect(option.optionId)}
              disabled={disabled}
              className={`group flex items-center justify-center gap-4 rounded-2xl border-2 p-5 text-left transition-all ${
                isSelected
                  ? "border-[#E87361] bg-white shadow-md"
                  : "border-transparent bg-white shadow-sm hover:border-[#E87361]/30 hover:shadow-md"
              } disabled:cursor-not-allowed ${
                isFiveOptions ? "flex-col lg:items-center" : "flex-row sm:items-start"
              }`}
            >
              <span
                className={`flex shrink-0 items-center justify-center font-bold transition-colors ${
                  isMultipleChoice ? "rounded-lg" : "rounded-full"
                } ${
                  isSelected
                    ? "bg-[#E87361] text-white"
                    : "bg-[#F3E8DF] text-[#A67E6B] group-hover:bg-[#E87361]/10 group-hover:text-[#E87361]"
                } ${isFiveOptions ? "h-14 w-14 text-xl" : "h-12 w-12 text-lg"}`}
              >
                {option.key}
              </span>
              <span
                className={`font-semibold leading-relaxed text-ink ${
                  isFiveOptions ? "text-center text-base lg:mt-2" : "text-base mt-3"
                }`}
              >
                {option.content}
              </span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

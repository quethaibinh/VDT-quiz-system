import { useRef, useEffect, useState } from "react";
import { MoreVertical, Eye, Edit, Archive, RefreshCw } from "lucide-react";
import type { Question } from "@/features/teacher/questions/model/question-types";
import { StatusChip } from "@/components/ui/status-chip";

const difficulty = { EASY: ["Dễ", "success"], MEDIUM: ["Vừa", "warning"], HARD: ["Khó", "danger"] } as const;

interface QuestionListProps {
  questions: Question[];
  selected?: Set<string>;
  onToggle?: (id: string) => void;
  allSelected?: boolean;
  someSelected?: boolean;
  onToggleAll?: () => void;
  disabled?: boolean;
  onView?: (id: string) => void;
  onEdit?: (id: string) => void;
  onArchive?: (id: string) => void;
  onRestore?: (id: string) => void;
}

export function QuestionList({
  questions,
  selected = new Set<string>(),
  onToggle,
  allSelected = false,
  someSelected = false,
  onToggleAll,
  disabled = false,
  onView,
  onEdit,
  onArchive,
  onRestore,
}: QuestionListProps) {
  const checkboxRef = useRef<HTMLInputElement>(null);
  const [activeDropdownId, setActiveDropdownId] = useState<string | null>(null);

  useEffect(() => {
    if (checkboxRef.current) {
      checkboxRef.current.indeterminate = someSelected;
    }
  }, [someSelected]);

  // Close dropdown on click outside
  useEffect(() => {
    if (!activeDropdownId) return;
    const handleOutsideClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (!target.closest(`.dropdown-trigger-${activeDropdownId}`) && !target.closest(`.dropdown-menu-${activeDropdownId}`)) {
        setActiveDropdownId(null);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [activeDropdownId]);

  return (
    <div className="overflow-hidden rounded-xl border border-line bg-surface shadow-soft">
      <div className="hidden grid-cols-[40px_1fr_140px_120px_120px_120px_60px] gap-4 border-b border-line px-5 py-3 text-xs font-semibold text-muted md:grid">
        <span>
          {onToggleAll && (
            <input
              ref={checkboxRef}
              type="checkbox"
              checked={allSelected}
              onChange={onToggleAll}
              disabled={disabled}
              aria-label="Chọn tất cả câu hỏi"
            />
          )}
        </span>
        <span>Nội dung câu hỏi</span>
        <span>Loại</span>
        <span>Độ khó</span>
        <span>Hiển thị</span>
        <span>Trạng thái</span>
        <span className="text-right">Hành động</span>
      </div>
      {questions.map((question) => {
        const [label, tone] = difficulty[question.difficulty];
        return (
          <article
            key={question.id}
            className={`grid gap-3 border-b border-line px-4 py-4 last:border-0 md:grid-cols-[40px_1fr_140px_120px_120px_120px_60px] md:items-center ${
              question.status === "ARCHIVED" ? "opacity-60 bg-slate-50/50 dark:bg-slate-900/5" : ""
            }`}
          >
            <input
              aria-label={`Chọn câu hỏi ${question.content}`}
              type="checkbox"
              checked={selected.has(question.id)}
              onChange={() => onToggle?.(question.id)}
              disabled={disabled}
            />
            <p className="m-0 text-sm leading-6 font-medium whitespace-pre-wrap">{question.content}</p>
            <span className="text-xs text-muted">
              {question.questionType === "SINGLE_CHOICE" ? "Một đáp án" : "Nhiều đáp án"}
            </span>
            <StatusChip tone={tone}>{label}</StatusChip>
            <StatusChip tone={question.visibility === "PUBLIC" ? "success" : "neutral"}>
              {question.visibility === "PUBLIC" ? "Công khai" : "Riêng tư"}
            </StatusChip>
            <StatusChip tone={question.status === "ACTIVE" ? "success" : "neutral"}>
              {question.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}
            </StatusChip>

            <div className="relative flex justify-end">
              <button
                type="button"
                className={`dropdown-trigger-${question.id} p-1.5 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 text-muted hover:text-ink transition-colors cursor-pointer`}
                onClick={() => setActiveDropdownId(activeDropdownId === question.id ? null : question.id)}
                aria-label="Menu hành động"
              >
                <MoreVertical className="h-4.5 w-4.5" />
              </button>
              {activeDropdownId === question.id && (
                <div
                  className={`dropdown-menu-${question.id} absolute right-0 top-8 z-30 w-48 rounded-xl border border-line bg-surface py-1 shadow-soft text-sm animate-in fade-in slide-in-from-top-1 duration-150`}
                >
                  <button
                    type="button"
                    onClick={() => {
                      onView?.(question.id);
                      setActiveDropdownId(null);
                    }}
                    className="flex w-full items-center gap-2 px-3.5 py-2 hover:bg-slate-50 dark:hover:bg-slate-900 text-left text-ink cursor-pointer"
                  >
                    <Eye className="h-4 w-4 text-muted" />
                    Xem chi tiết
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      onEdit?.(question.id);
                      setActiveDropdownId(null);
                    }}
                    className="flex w-full items-center gap-2 px-3.5 py-2 hover:bg-slate-50 dark:hover:bg-slate-900 text-left text-ink cursor-pointer"
                  >
                    <Edit className="h-4 w-4 text-muted" />
                    Chỉnh sửa
                  </button>
                  {question.status === "ACTIVE" ? (
                    <button
                      type="button"
                      onClick={() => {
                        onArchive?.(question.id);
                        setActiveDropdownId(null);
                      }}
                      className="flex w-full items-center gap-2 px-3.5 py-2 hover:bg-slate-50 dark:hover:bg-slate-900 text-left text-rose-600 dark:text-rose-400 cursor-pointer"
                    >
                      <Archive className="h-4 w-4 text-rose-500" />
                      Lưu trữ
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => {
                        onRestore?.(question.id);
                        setActiveDropdownId(null);
                      }}
                      className="flex w-full items-center gap-2 px-3.5 py-2 hover:bg-slate-50 dark:hover:bg-slate-900 text-left text-emerald-600 dark:text-emerald-400 cursor-pointer"
                    >
                      <RefreshCw className="h-4 w-4 text-emerald-500" />
                      Khôi phục
                    </button>
                  )}
                </div>
              )}
            </div>
          </article>
        );
      })}
    </div>
  );
}


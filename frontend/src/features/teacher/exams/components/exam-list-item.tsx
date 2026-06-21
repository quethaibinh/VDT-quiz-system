import { ChevronDown } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { ExamInlineDetails } from "@/features/teacher/exams/components/exam-inline-details";
import type { ExamStatus, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const status: Record<ExamStatus, [string, "neutral" | "success" | "warning" | "danger"]> = {
  DRAFT: ["Bản nháp", "neutral"],
  SCHEDULED: ["Chờ bắt đầu", "warning"],
  ACTIVE: ["Đang diễn ra", "success"],
  CLOSED: ["Đã kết thúc", "neutral"],
  CANCELLED: ["Đã hủy", "danger"],
};

interface ExamListItemProps {
  exam: ExamSummary;
  expanded?: boolean;
  onToggle?: () => void;
  cancelling?: boolean;
  onCancel?: (examId: string) => void;
  scheduling?: boolean;
  scheduleDisabledReason?: string | null;
  onSchedule?: (exam: ExamSummary) => void;
}

export function ExamListItem({
  exam,
  expanded = false,
  onToggle,
  cancelling,
  onCancel,
  scheduling,
  scheduleDisabledReason,
  onSchedule,
}: ExamListItemProps) {
  const isDraft = exam.status === "DRAFT";
  const panelId = `exam-details-${exam.id}`;
  const triggerId = `exam-trigger-${exam.id}`;
  const reasonId = `exam-schedule-reason-${exam.id}`;

  return (
    <article className="overflow-hidden rounded-xl border border-line bg-surface shadow-soft">
      <div className="grid gap-4 p-5 lg:grid-cols-[1fr_auto] lg:items-center">
        <button
          type="button"
          id={triggerId}
          className="flex min-w-0 items-center gap-3 rounded-lg text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          aria-expanded={expanded}
          aria-controls={expanded ? panelId : undefined}
          onClick={onToggle}
        >
          <span className="min-w-0 flex-1">
            <span className="block truncate text-2xl font-bold text-ink">{exam.title}</span>
            <span className="mt-2 block text-sm text-muted">
              {new Date(exam.startAt).toLocaleString("vi-VN")} · {exam.durationMinutes} phút · {exam.questionCount} câu
            </span>
          </span>
          <StatusChip tone={status[exam.status][1]}>{status[exam.status][0]}</StatusChip>
          <ChevronDown
            aria-hidden="true"
            className={`h-5 w-5 shrink-0 text-muted transition-transform ${expanded ? "rotate-180" : ""}`}
          />
        </button>

        {isDraft && (
          <div className="flex flex-col items-start gap-2 lg:items-end">
            <div className="flex flex-wrap gap-2">
              <Button
                loading={scheduling}
                disabled={!onSchedule || Boolean(scheduleDisabledReason)}
                aria-describedby={scheduleDisabledReason ? reasonId : undefined}
                onClick={() => onSchedule?.(exam)}
              >
                Lên lịch
              </Button>
              <Link
                to={`/teacher/subjects/${exam.subjectId}/exams/${exam.id}/edit`}
                className="inline-flex min-h-10 items-center justify-center rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold text-ink transition hover:border-primary"
              >
                Chỉnh sửa
              </Link>
              <Button
                variant="danger"
                loading={cancelling}
                disabled={!onCancel}
                onClick={() => onCancel?.(exam.id)}
              >
                Hủy ca thi
              </Button>
            </div>
            {scheduleDisabledReason && (
              <p id={reasonId} className="m-0 max-w-sm text-xs text-warning">
                {scheduleDisabledReason}
              </p>
            )}
          </div>
        )}
      </div>

      {expanded && (
        <ExamInlineDetails
          subjectId={exam.subjectId}
          examId={exam.id}
          panelId={panelId}
          labelledBy={triggerId}
        />
      )}
    </article>
  );
}

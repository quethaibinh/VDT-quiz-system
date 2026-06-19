import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
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
  cancelling?: boolean;
  onCancel?: (examId: string) => void;
  activating?: boolean;
  onActivate?: (examId: string) => void;
}

export function ExamListItem({ exam, cancelling, onCancel }: ExamListItemProps) {
  const isDraft = exam.status === "DRAFT";

  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft lg:grid-cols-[1fr_auto_auto] lg:items-center">
      <div>
        <h2 className="m-0 text-2xl">{exam.title}</h2>
        <p className="mb-0 mt-2 text-sm text-muted">
          {new Date(exam.startAt).toLocaleString("vi-VN")} · {exam.durationMinutes} phút · {exam.questionCount} câu
        </p>
      </div>
      <StatusChip tone={status[exam.status][1]}>{status[exam.status][0]}</StatusChip>
      {isDraft && (
        <div className="flex flex-wrap gap-2">
          <Link to={`/teacher/subjects/${exam.subjectId}/exams/${exam.id}/edit`}>
            <Button variant="secondary">Chỉnh sửa</Button>
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
      )}
    </article>
  );
}

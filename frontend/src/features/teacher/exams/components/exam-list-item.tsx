import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import type { ExamStatus, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const status: Record<ExamStatus, [string, "neutral" | "success" | "warning"]> = {
  DRAFT: ["Bản nháp", "neutral"],
  SCHEDULED: ["Chờ bắt đầu", "warning"],
  ACTIVE: ["Đang diễn ra", "success"],
  CLOSED: ["Đã kết thúc", "neutral"],
};

interface ExamListItemProps {
  exam: ExamSummary;
  activating?: boolean;
  onActivate: (examId: string) => void;
}

export function ExamListItem({ exam, activating, onActivate }: ExamListItemProps) {
  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft lg:grid-cols-[1fr_auto_auto] lg:items-center">
      <div>
        <h2 className="m-0 text-2xl">{exam.title}</h2>
        <p className="mb-0 mt-2 text-sm text-muted">
          {new Date(exam.startAt).toLocaleString("vi-VN")} · {exam.durationMinutes} phút · {exam.questionCount} câu
        </p>
      </div>
      <StatusChip tone={status[exam.status][1]}>{status[exam.status][0]}</StatusChip>
      <div className="flex flex-wrap gap-2">
        {exam.status === "DRAFT" && <Button variant="secondary" loading={activating} onClick={() => onActivate(exam.id)}>Kích hoạt</Button>}
        <Link to={`/teacher/exams/${exam.id}/monitor`}><Button variant="secondary">Giám sát</Button></Link>
        {exam.status === "CLOSED" && <Link to={`/teacher/exams/${exam.id}/results`}><Button variant="secondary">Kết quả</Button></Link>}
      </div>
    </article>
  );
}

import { Link } from "react-router-dom";
import { CalendarDays, Clock, HelpCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import type { StudentExamSummary } from "../model/student-exam-contracts";

function formatDateTime(isoString: string) {
  return new Date(isoString).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

interface StudentExamCardProps {
  exam: StudentExamSummary;
}

export function StudentExamCard({ exam }: StudentExamCardProps) {
  const { examId, title, subjectName, startAt, durationMinutes, questionCount, studentAvailability } = exam;

  const statusConfig = {
    UPCOMING: { text: "Sắp diễn ra", tone: "neutral" as const },
    OPEN: { text: "Đang mở", tone: "success" as const },
    ENDED: { text: "Đã kết thúc", tone: "danger" as const },
  };

  const currentStatus = statusConfig[studentAvailability] ?? { text: studentAvailability, tone: "neutral" as const };

  return (
    <article className="flex h-full flex-col justify-between space-y-4 rounded-xl border border-line bg-surface p-5 shadow-soft transition hover:shadow-md">
      <div className="space-y-2">
        <div className="flex items-start justify-between gap-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-muted">{subjectName}</span>
          <StatusChip tone={currentStatus.tone}>{currentStatus.text}</StatusChip>
        </div>
        <h3 className="line-clamp-2 text-lg font-bold leading-snug text-ink">{title}</h3>
      </div>

      <div className="space-y-2 text-sm text-muted">
        <div className="flex items-center gap-2">
          <CalendarDays size={16} className="text-muted" />
          <span>Bắt đầu: {formatDateTime(startAt)}</span>
        </div>
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-1.5">
            <Clock size={16} className="text-muted" />
            <span>{durationMinutes} phút</span>
          </div>
          <div className="flex items-center gap-1.5">
            <HelpCircle size={16} className="text-muted" />
            <span>{questionCount} câu hỏi</span>
          </div>
        </div>
      </div>

      <div className="flex gap-2 pt-2">
        {studentAvailability === "UPCOMING" && (
          <Link to={`${examId}/lobby`} className="w-full">
            <Button variant="secondary" className="w-full">Vào phòng chờ</Button>
          </Link>
        )}
        {studentAvailability === "OPEN" && (
          <Link to={`${examId}/lobby`} className="w-full">
            <Button variant="primary" className="w-full">Vào thi</Button>
          </Link>
        )}
        {studentAvailability === "ENDED" && (
          <div className="flex w-full gap-2">
            <Button variant="secondary" disabled className="w-full">Đã kết thúc</Button>
            <Button variant="ghost" disabled className="w-full text-xs">Kết quả chưa hỗ trợ</Button>
          </div>
        )}
      </div>
    </article>
  );
}

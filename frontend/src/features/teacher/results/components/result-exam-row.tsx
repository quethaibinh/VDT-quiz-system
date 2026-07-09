import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import type { ExamSummary } from "@/features/teacher/exams";

export function ResultExamRow({ exam, backTo }: { exam: ExamSummary; backTo: string }) {
  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:grid-cols-[1fr_auto] md:items-center">
      <div>
        <h2 className="m-0 text-xl">{exam.title}</h2>
        <p className="mb-0 mt-2 text-sm text-muted">
          Kết thúc · {new Date(exam.startAt).toLocaleString("vi-VN")} · {exam.assignedCount} học sinh
        </p>
      </div>
      <Link
        to={`/teacher/exams/${exam.id}/results`}
        state={{ backTo, examTitle: exam.title }}
        className="inline-flex items-center gap-2 font-semibold text-primary"
      >
        Mở báo cáo <ArrowRight className="h-4 w-4" />
      </Link>
    </article>
  );
}

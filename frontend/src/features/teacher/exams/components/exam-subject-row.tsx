import { ArrowRight, CalendarDays } from "lucide-react";
import { Link } from "react-router-dom";
import type { Subject } from "@/features/teacher/subjects";

export function ExamSubjectRow({ subject }: { subject: Subject }) {
  return (
    <article className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:flex-row md:items-center">
      <div className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary"><CalendarDays /></div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="m-0 text-xl">{subject.name}</h2>
          <span className="rounded-full bg-canvas px-3 py-1 text-xs font-semibold text-primary">{subject.code}</span>
        </div>
        <p className="mb-0 mt-2 text-sm text-muted">{subject.description || "Chưa có mô tả môn học."}</p>
      </div>
      <Link to={`/teacher/subjects/${subject.id}/exams`} className="inline-flex items-center gap-2 self-start font-semibold text-primary md:self-auto">
        Xem ca thi <ArrowRight className="h-4 w-4" />
      </Link>
    </article>
  );
}

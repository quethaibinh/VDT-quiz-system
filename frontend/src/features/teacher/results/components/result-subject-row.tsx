import { ArrowRight, ChartNoAxesColumn } from "lucide-react";
import { Link } from "react-router-dom";
import type { Subject } from "@/features/teacher/subjects";

export function ResultSubjectRow({ subject }: { subject: Subject }) {
  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:grid-cols-[auto_1fr_auto] md:items-center">
      <div className="grid h-11 w-11 place-items-center rounded-full bg-accent/10 text-accent"><ChartNoAxesColumn /></div>
      <div>
        <p className="m-0 text-xs font-semibold uppercase tracking-wider text-muted">Báo cáo môn học · {subject.code}</p>
        <h2 className="mb-1 mt-1 text-xl">{subject.name}</h2>
        <p className="m-0 text-sm text-muted">Xem các ca thi đã kết thúc và kết quả chấm điểm.</p>
      </div>
      <Link to={`/teacher/subjects/${subject.id}/results`} className="inline-flex items-center gap-2 font-semibold text-primary">
        Xem kết quả <ArrowRight className="h-4 w-4" />
      </Link>
    </article>
  );
}

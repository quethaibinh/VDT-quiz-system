import { ArrowRight, BookOpen } from "lucide-react";
import { Link } from "react-router-dom";
import type { Subject } from "@/features/teacher/subjects/model/subject-types";

export function SubjectCard({ subject }: { subject: Subject }) {
  return (
    <article className="rounded-xl border border-line bg-surface p-6 shadow-soft">
      <div className="flex items-start justify-between">
        <div className="grid h-11 w-11 place-items-center rounded-lg bg-primary/10 text-primary"><BookOpen /></div>
        <span className="text-xs font-semibold text-success">Hoạt động</span>
      </div>
      <h2 className="mb-1 mt-6 text-2xl">{subject.name}</h2>
      <p className="text-sm font-semibold text-primary">{subject.code}</p>
      <p className="min-h-12 text-sm leading-6 text-muted">{subject.description || "Chưa có mô tả môn học."}</p>
      <Link className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-primary" to={`/teacher/subjects/${subject.id}`}>
        Mở không gian môn <ArrowRight className="h-4 w-4" />
      </Link>
    </article>
  );
}

import { ChevronRight, Zap } from "lucide-react";
import { Link } from "react-router-dom";
import type { Subject } from "@/features/teacher/subjects";

export function LiveQuizSubjectRow({ subject }: { subject: Subject }) {
  return (
    <Link
      to={`/teacher/subjects/${subject.id}/live-quizzes`}
      className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft transition hover:border-primary md:grid-cols-[1fr_auto] md:items-center"
    >
      <span className="flex min-w-0 items-start gap-3">
        <span className="mt-1 rounded-lg bg-primary/10 p-2 text-primary">
          <Zap className="h-5 w-5" />
        </span>
        <span className="min-w-0">
          <span className="block text-2xl font-bold text-ink">{subject.name}</span>
          <span className="mt-1 block text-sm text-muted">
            {subject.code} · Tạo và điều khiển quiz trực tiếp trong lớp
          </span>
        </span>
      </span>
      <span className="inline-flex items-center gap-2 text-sm font-semibold text-primary">
        Mở danh sách
        <ChevronRight className="h-4 w-4" />
      </span>
    </Link>
  );
}

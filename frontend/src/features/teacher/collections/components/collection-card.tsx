import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { StatusChip } from "@/components/ui/status-chip";
import type { QuestionCollection } from "@/features/teacher/collections/model/collection-types";

export function CollectionCard({ collection }: { collection: QuestionCollection }) {
  return <article className="rounded-xl border border-line bg-surface p-6 shadow-soft">
    <div className="flex flex-wrap gap-2"><StatusChip>{collection.visibility === "PRIVATE" ? "Riêng tư" : "Công khai"}</StatusChip><StatusChip tone={collection.editable ? "success" : "neutral"}>{collection.editable ? "Có thể chỉnh sửa" : "Chỉ xem"}</StatusChip></div>
    <h2 className="mb-2 mt-5 text-2xl">{collection.name}</h2>
    <p className="min-h-10 text-sm text-muted">{collection.description || "Chưa có mô tả."}</p>
    <strong className="text-2xl">{collection.stats.questionCount}</strong><span className="ml-2 text-sm text-muted">câu hỏi</span>
    <div className="mt-3 flex gap-4 text-xs"><span className="text-success">{collection.stats.easy} Dễ</span><span className="text-warning">{collection.stats.medium} Vừa</span><span className="text-danger">{collection.stats.hard} Khó</span></div>
    <Link className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-primary" to={collection.id}>Mở bộ <ArrowRight className="h-4 w-4" /></Link>
  </article>;
}

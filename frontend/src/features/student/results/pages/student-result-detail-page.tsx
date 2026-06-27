import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { Award, CheckCircle2, CircleSlash, Medal, XCircle } from "lucide-react";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { studentResultRepository } from "@/features/student/results/api/student-result-repository";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentResultDetailPage() {
  const { examId = "" } = useParams();
  const query = useQuery({
    queryKey: ["student", "results", examId],
    queryFn: () => studentResultRepository.getResult(examId),
    enabled: Boolean(examId),
  });
  const result = query.data;
  const visible = result?.visibilityState === "READY" || result?.visibilityState === "RELEASED";

  return (
    <div className="space-y-6">
      <PageHeader
        title={result?.title || "Chi tiết kết quả"}
        description={result?.subjectName || "Tổng hợp điểm cá nhân."}
        action={<Link to="/student/results"><Button variant="secondary">Quay lại</Button></Link>}
      />
      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={query.isSuccess && !query.data}
        onRetry={() => void query.refetch()}
      >
        {result && visible && (
          <>
            <div className="grid gap-3 md:grid-cols-4">
              <Metric icon={<Award size={18} />} label="Điểm" value={`${result.score?.toFixed(2)} / ${result.maxScore?.toFixed(2)}`} />
              <Metric icon={<Medal size={18} />} label="Xếp hạng" value={`#${result.rank}`} />
              <Metric icon={<CheckCircle2 size={18} />} label="Đúng" value={String(result.correctCount)} />
              <Metric icon={<XCircle size={18} />} label="Sai" value={String(result.wrongCount)} />
            </div>
            <section className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:grid-cols-3">
              <Detail label="Phần trăm" value={`${result.percentage?.toFixed(1)}%`} />
              <Detail label="Bỏ trống" value={String(result.blankCount)} />
              <Detail label="Tổng số câu" value={String(result.totalQuestions)} />
              <Detail label="Nộp lúc" value={new Date(result.submittedAt).toLocaleString("vi-VN")} />
              <Detail label="Chấm lúc" value={new Date(result.gradedAt).toLocaleString("vi-VN")} />
              <Detail label="Phát hành" value={result.releasedAt ? new Date(result.releasedAt).toLocaleString("vi-VN") : "Theo chính sách ca thi"} />
            </section>
          </>
        )}
        {result && !visible && (
          <section className="rounded-xl border border-line bg-surface p-8 text-center shadow-soft">
            <CircleSlash className="mx-auto mb-3 text-warning" size={42} />
            <h2 className="m-0 text-xl">Chưa thể xem điểm</h2>
            <p className="text-muted">{result.message}</p>
          </section>
        )}
      </DataState>
    </div>
  );
}

function Metric({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return <div className="rounded-xl border border-line bg-surface p-5 shadow-soft"><div className="mb-2 text-primary">{icon}</div><strong className="text-2xl">{value}</strong><small className="block text-muted">{label}</small></div>;
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><small className="block text-muted">{label}</small><strong>{value}</strong></div>;
}

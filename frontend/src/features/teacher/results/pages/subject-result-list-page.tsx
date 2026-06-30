import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { listExams } from "@/features/teacher/exams";
import { ResultExamRow } from "@/features/teacher/results/components/result-exam-row";
import { subjectDetailQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function SubjectResultListPage() {
  const { subjectId = "" } = useParams();
  const subject = useQuery(subjectDetailQuery(subjectId));
  const exams = useQuery({
    queryKey: ["teacher", "results", subjectId, "closed-exams"],
    queryFn: () => listExams(subjectId, { status: "CLOSED", size: 100 }),
    enabled: Boolean(subjectId) && subject.isSuccess,
  });
  const backTo = `/teacher/subjects/${subjectId}/results`;
  const error = subject.error ?? exams.error;

  return (
    <div className="space-y-6">
      <PageHeader
        title={subject.data ? `Kết quả · ${subject.data.name}` : "Kết quả"}
        description="Danh sách ca thi đã kết thúc và sẵn sàng xem báo cáo."
        action={<Link to="/teacher/results"><Button variant="secondary">Đổi môn</Button></Link>}
      />
      <DataState
        loading={subject.isLoading || exams.isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={subject.isSuccess && !exams.data?.content.length}
        emptyMessage="Môn học này chưa có ca thi đã kết thúc."
        onRetry={() => { void subject.refetch(); void exams.refetch(); }}
      >
        <div className="space-y-3">{exams.data?.content.map((exam) => <ResultExamRow key={exam.id} exam={exam} backTo={backTo} />)}</div>
      </DataState>
    </div>
  );
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { activateExam, listExams } from "@/features/teacher/exams/api/exam-repository";
import { ExamListItem } from "@/features/teacher/exams/components/exam-list-item";
import { subjectDetailQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function SubjectExamListPage() {
  const { subjectId = "" } = useParams();
  const queryClient = useQueryClient();
  const subject = useQuery(subjectDetailQuery(subjectId));
  const exams = useQuery({
    queryKey: ["teacher", "exams", subjectId],
    queryFn: () => listExams({ subjectId }),
    enabled: Boolean(subjectId) && subject.isSuccess,
  });
  const activate = useMutation({
    mutationFn: activateExam,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["teacher", "exams", subjectId] }),
  });

  const error = subject.error ?? exams.error;
  return (
    <div className="space-y-6">
      <PageHeader
        title={subject.data ? `Ca thi · ${subject.data.name}` : "Ca thi"}
        description="Các ca thi của môn đã chọn. Dữ liệu hiện dùng contract mô phỏng."
        action={<div className="flex gap-2"><Link to="/teacher/exams"><Button variant="secondary">Đổi môn</Button></Link><Link to="new"><Button><Plus className="h-4 w-4" />Tạo ca thi</Button></Link></div>}
      />
      <p className="rounded-lg bg-warning/10 p-3 text-sm text-warning">Chế độ demo: Exam Service chưa được triển khai trong backend.</p>
      <DataState
        loading={subject.isLoading || exams.isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={subject.isSuccess && !exams.data?.length}
        emptyMessage="Môn học này chưa có ca thi."
        onRetry={() => { void subject.refetch(); void exams.refetch(); }}
      >
        <div className="space-y-3">
          {exams.data?.map((exam) => <ExamListItem key={exam.id} exam={exam} activating={activate.isPending && activate.variables === exam.id} onActivate={(id) => activate.mutate(id)} />)}
        </div>
      </DataState>
    </div>
  );
}

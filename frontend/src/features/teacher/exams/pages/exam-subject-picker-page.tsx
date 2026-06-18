import { useQuery } from "@tanstack/react-query";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { ExamSubjectRow } from "@/features/teacher/exams/components/exam-subject-row";
import { subjectListQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function ExamSubjectPickerPage() {
  const query = useQuery(subjectListQuery());

  return (
    <div className="space-y-7">
      <PageHeader title="Ca thi" description="Chọn môn học để xem và quản lý các ca thi thuộc môn đó." />
      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.length} emptyMessage="Bạn chưa được phân công môn học để tạo ca thi." onRetry={() => query.refetch()}>
        <div className="space-y-3">{query.data?.map((subject) => <ExamSubjectRow key={subject.id} subject={subject} />)}</div>
      </DataState>
    </div>
  );
}

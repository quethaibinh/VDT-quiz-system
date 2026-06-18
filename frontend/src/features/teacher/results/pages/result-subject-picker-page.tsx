import { useQuery } from "@tanstack/react-query";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { ResultSubjectRow } from "@/features/teacher/results/components/result-subject-row";
import { subjectListQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function ResultSubjectPickerPage() {
  const query = useQuery(subjectListQuery());

  return (
    <div className="space-y-7">
      <PageHeader title="Kết quả" description="Chọn môn học để xem các ca thi đã kết thúc và báo cáo điểm." />
      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.length} emptyMessage="Bạn chưa được phân công môn học để xem kết quả." onRetry={() => query.refetch()}>
        <div className="space-y-3">{query.data?.map((subject) => <ResultSubjectRow key={subject.id} subject={subject} />)}</div>
      </DataState>
    </div>
  );
}

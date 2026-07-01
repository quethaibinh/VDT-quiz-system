import { useQuery } from "@tanstack/react-query";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { LiveQuizSubjectRow } from "@/features/teacher/live-quizzes/components/live-quiz-subject-row";
import { subjectListQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizSubjectPickerPage() {
  const query = useQuery(subjectListQuery());

  return (
    <div className="space-y-7">
      <PageHeader
        title="Quiz"
        description="Chon mon hoc de tao quiz truc tiep, chuan bi ma phong va mo phong khi lop san sang."
      />
      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={!query.data?.length}
        emptyMessage="Ban chua duoc phan cong mon hoc de tao quiz."
        onRetry={() => query.refetch()}
      >
        <div className="space-y-3">{query.data?.map((subject) => <LiveQuizSubjectRow key={subject.id} subject={subject} />)}</div>
      </DataState>
    </div>
  );
}

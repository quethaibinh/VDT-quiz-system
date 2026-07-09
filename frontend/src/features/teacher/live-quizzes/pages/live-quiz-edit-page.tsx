import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { getLiveQuiz, liveQuizKeys, updateLiveQuiz } from "@/features/teacher/live-quizzes";
import { LiveQuizForm } from "@/features/teacher/live-quizzes/components/live-quiz-form";
import type { LiveQuizRequest } from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizEditPage() {
  const { subjectId = "", quizId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const detail = useQuery({
    queryKey: liveQuizKeys.detail(subjectId, quizId),
    queryFn: () => getLiveQuiz(subjectId, quizId),
    enabled: Boolean(subjectId && quizId),
  });
  const save = useMutation({
    mutationFn: (input: LiveQuizRequest) => updateLiveQuiz(subjectId, quizId, input),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.subject(subjectId) });
      navigate(`/teacher/subjects/${subjectId}/live-quizzes`);
    },
  });

  if (detail.data && detail.data.status !== "DRAFT") {
    return <Navigate to={`/teacher/subjects/${subjectId}/live-quizzes`} replace />;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Chỉnh sửa quiz" description={detail.data?.title ?? "Đang tải bản nháp..."} />
      <DataState
        loading={detail.isLoading}
        error={detail.error ? getApiErrorMessage(detail.error) : null}
        empty={!detail.data}
        onRetry={() => detail.refetch()}
      >
        {detail.data && (
          <LiveQuizForm
            key={detail.data.id}
            subjectId={subjectId}
            initial={detail.data}
            submitLabel="Lưu thay đổi"
            submitting={save.isPending}
            error={save.error ? getApiErrorMessage(save.error) : null}
            onSubmit={(input) => save.mutate(input)}
          />
        )}
      </DataState>
    </div>
  );
}

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { createLiveQuiz, liveQuizKeys } from "@/features/teacher/live-quizzes";
import { LiveQuizForm } from "@/features/teacher/live-quizzes/components/live-quiz-form";
import type { LiveQuizRequest } from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizBuilderPage() {
  const { subjectId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (input: LiveQuizRequest) => createLiveQuiz(subjectId, input),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.subject(subjectId) });
      navigate(`/teacher/subjects/${subjectId}/live-quizzes`);
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tao quiz"
        description="Cau hinh quiz truc tiep tu mot bo cau hoi. Tat ca cau hoi trong bo se duoc dong bang khi prepare."
      />
      <LiveQuizForm
        subjectId={subjectId}
        submitLabel="Tao quiz"
        submitting={save.isPending}
        error={save.error ? getApiErrorMessage(save.error) : null}
        onSubmit={(input) => save.mutate(input)}
      />
    </div>
  );
}

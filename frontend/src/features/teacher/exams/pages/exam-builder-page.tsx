import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { addAssignments } from "@/features/teacher/exams/api/assignment-repository";
import { createExam, examKeys } from "@/features/teacher/exams/api/exam-repository";
import {
  getExamRecoveryKey,
  toExamDraftInput,
  type ExamBuilderState,
} from "@/features/teacher/exams/components/builder/builder-state";
import { ExamBuilderForm } from "@/features/teacher/exams/components/builder/exam-builder-form";
import type { StudentSummary } from "@/features/teacher/exams/model/exam-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function ExamBuilderPage() {
  const { subjectId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [partialMessage, setPartialMessage] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: async (state: ExamBuilderState) => {
      const exam = await createExam(subjectId, toExamDraftInput(state));
      try {
        await addAssignments(subjectId, exam.id, [...state.selectedStudents.keys()]);
      } catch (error) {
        // Luu snapshot tam de refresh trang edit van co the khoi phuc danh sach chua gan.
        saveRecoveryStudents(exam.id, [...state.selectedStudents.values()]);
        throw { cause: error, examId: exam.id, examTitle: exam.title };
      }
      return exam;
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: examKeys.subject(subjectId) });
      navigate(`/teacher/subjects/${subjectId}/exams`);
    },
    onError: (error: { examId?: string; examTitle?: string }) => {
      if (!error.examId) return;
      setPartialMessage(
        `Bản nháp “${error.examTitle ?? ""}” đã được tạo nhưng chưa phân công đủ học sinh. Bạn sẽ được chuyển sang trang chỉnh sửa để thử lại mà không tạo thêm bản nháp.`,
      );
      window.setTimeout(() => {
        navigate(`/teacher/subjects/${subjectId}/exams/${error.examId}/edit`, {
          state: { partialCreate: true },
        });
      }, 1200);
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tạo ca thi"
        description="Hoàn thiện cấu hình và phân công học sinh cho bản nháp mới."
      />
      <ExamBuilderForm
        subjectId={subjectId}
        submitLabel="Tạo bản nháp và phân công"
        submitting={save.isPending}
        notice={partialMessage}
        error={save.error && !partialMessage ? getApiErrorMessage(save.error) : null}
        onSubmit={(state) => save.mutate(state)}
      />
    </div>
  );
}

function saveRecoveryStudents(examId: string, students: StudentSummary[]) {
  sessionStorage.setItem(getExamRecoveryKey(examId), JSON.stringify(students));
}

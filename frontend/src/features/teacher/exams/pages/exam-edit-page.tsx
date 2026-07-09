import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import {
  addAssignments,
  assignmentKeys,
  listAssignments,
  removeAssignment,
} from "@/features/teacher/exams/api/assignment-repository";
import { examKeys, getExam, updateExam } from "@/features/teacher/exams/api/exam-repository";
import {
  getExamRecoveryKey,
  hydrateBuilderState,
  reconcileStudentIds,
  toExamDraftInput,
  type ExamBuilderState,
} from "@/features/teacher/exams/components/builder/builder-state";
import { ExamBuilderForm } from "@/features/teacher/exams/components/builder/exam-builder-form";
import type { StudentSummary } from "@/features/teacher/exams/model/exam-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function ExamEditPage() {
  const { subjectId = "", examId = "" } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [partialMessage, setPartialMessage] = useState<string | null>(null);
  const detail = useQuery({
    queryKey: examKeys.detail(subjectId, examId),
    queryFn: () => getExam(subjectId, examId),
    enabled: Boolean(subjectId && examId),
  });
  const assignments = useQuery({
    queryKey: assignmentKeys.list(subjectId, examId),
    queryFn: () => listAssignments(subjectId, examId, { page: 0, size: 100 }),
    enabled: detail.data?.status === "DRAFT",
  });

  const initialStudents = useMemo(() => {
    const serverStudents: StudentSummary[] = (assignments.data?.content ?? []).map((assignment) => ({
      id: assignment.studentId,
      studentCode: assignment.studentCode,
      fullName: assignment.studentName,
      displayName: assignment.studentName,
    }));
    const recovered = readRecoveryStudents(examId);
    return [...new Map([...serverStudents, ...recovered].map((student) => [student.id, student])).values()];
  }, [assignments.data, examId]);
  const originalIds = useMemo(
    () => new Set((assignments.data?.content ?? []).map((assignment) => assignment.studentId)),
    [assignments.data],
  );
  const initialState = useMemo(
    () => detail.data && assignments.data ? hydrateBuilderState(detail.data, initialStudents) : null,
    [assignments.data, detail.data, initialStudents],
  );

  const save = useMutation({
    mutationFn: async (state: ExamBuilderState) => {
      await updateExam(subjectId, examId, toExamDraftInput(state));
      const diff = reconcileStudentIds(originalIds, state.selectedStudents.keys());
      const operations: Promise<unknown>[] = [
        ...(diff.added.length > 0 ? [addAssignments(subjectId, examId, diff.added)] : []),
        ...diff.removed.map((studentId) => removeAssignment(subjectId, examId, studentId)),
      ];
      const results = await Promise.allSettled(operations);
      const failed = results.filter(({ status }) => status === "rejected").length;
      if (failed > 0) throw new Error(`Cấu hình đã lưu nhưng ${failed} thay đổi phân công chưa hoàn tất.`);
    },
    onSuccess: async () => {
      sessionStorage.removeItem(getExamRecoveryKey(examId));
      await invalidateExamQueries(queryClient, subjectId, examId);
      navigate(`/teacher/subjects/${subjectId}/exams`);
    },
    onError: async (error) => {
      // Refetch de lan thu lai tinh diff tu trang thai server moi nhat.
      await invalidateExamQueries(queryClient, subjectId, examId);
      setPartialMessage(`${getApiErrorMessage(error)} Dữ liệu đã được làm mới; hãy kiểm tra danh sách và thử lưu lại.`);
    },
  });

  if (detail.data && detail.data.status !== "DRAFT") {
    return <Navigate to={`/teacher/subjects/${subjectId}/exams`} replace />;
  }

  const error = detail.error ?? assignments.error;
  const recoveryNotice = (location.state as { partialCreate?: boolean } | null)?.partialCreate
    ? "Bản nháp đã tồn tại. Hãy kiểm tra danh sách học sinh và lưu lại; hệ thống sẽ không tạo thêm ca thi."
    : null;

  return (
    <div className="space-y-6">
      <PageHeader title="Chỉnh sửa ca thi" description={detail.data?.title ?? "Đang tải bản nháp..."} />
      <DataState
        loading={detail.isLoading || (detail.data?.status === "DRAFT" && assignments.isLoading)}
        error={error ? getApiErrorMessage(error) : null}
        onRetry={() => {
          void detail.refetch();
          void assignments.refetch();
        }}
      >
        {initialState && (
          <ExamBuilderForm
            subjectId={subjectId}
            initialState={initialState}
            submitLabel="Lưu thay đổi"
            submitting={save.isPending}
            notice={partialMessage ?? recoveryNotice}
            error={save.error && !partialMessage ? getApiErrorMessage(save.error) : null}
            onSubmit={(state) => save.mutate(state)}
          />
        )}
      </DataState>
    </div>
  );
}

function readRecoveryStudents(examId: string): StudentSummary[] {
  try {
    return JSON.parse(sessionStorage.getItem(getExamRecoveryKey(examId)) ?? "[]") as StudentSummary[];
  } catch {
    return [];
  }
}

async function invalidateExamQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  subjectId: string,
  examId: string,
) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: examKeys.subject(subjectId) }),
    queryClient.invalidateQueries({ queryKey: examKeys.detail(subjectId, examId) }),
    queryClient.invalidateQueries({ queryKey: assignmentKeys.list(subjectId, examId) }),
  ]);
}

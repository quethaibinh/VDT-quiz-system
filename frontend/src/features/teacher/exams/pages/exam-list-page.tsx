import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import {
  cancelExam,
  examKeys,
  listExams,
  scheduleExam,
} from "@/features/teacher/exams/api/exam-repository";
import { ExamListItem } from "@/features/teacher/exams/components/exam-list-item";
import {
  getScheduleDisabledReason,
  getScheduleErrorMessage,
} from "@/features/teacher/exams/lib/exam-scheduling";
import type { ExamStatus, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";
import { subjectDetailQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

const PAGE_SIZE = 10;

export function SubjectExamListPage() {
  const { subjectId = "" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [cancelTarget, setCancelTarget] = useState<ExamSummary | null>(null);
  const [scheduleTarget, setScheduleTarget] = useState<ExamSummary | null>(null);
  const [expandedExamId, setExpandedExamId] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const queryClient = useQueryClient();
  const page = Math.max(0, Number(searchParams.get("page") ?? 0) || 0);
  const keyword = searchParams.get("keyword") ?? "";
  const status = (searchParams.get("status") ?? "") as ExamStatus | "";
  const subject = useQuery(subjectDetailQuery(subjectId));
  const params = {
    page,
    size: PAGE_SIZE,
    keyword: keyword || undefined,
    status: status || undefined,
  };
  const exams = useQuery({
    queryKey: examKeys.list(subjectId, params),
    queryFn: () => listExams(subjectId, params),
    enabled: Boolean(subjectId) && subject.isSuccess,
  });
  const cancel = useMutation({
    mutationFn: (examId: string) => cancelExam(subjectId, examId),
    onSuccess: async () => {
      setCancelTarget(null);
      await queryClient.invalidateQueries({ queryKey: examKeys.subject(subjectId) });
    },
  });
  const schedule = useMutation({
    mutationFn: (examId: string) => scheduleExam(subjectId, examId),
    onSuccess: async (scheduledExam) => {
      cancel.reset();
      setScheduleTarget(null);
      setSuccessMessage(`Đã lên lịch ca thi “${scheduledExam.title}”.`);
      await queryClient.invalidateQueries({ queryKey: examKeys.subject(subjectId) });
    },
    onError: async () => {
      await queryClient.invalidateQueries({ queryKey: examKeys.subject(subjectId) });
    },
  });

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);

  const updateSearchParam = (key: string, value: string, resetPage = false) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (resetPage) next.delete("page");
    setSearchParams(next);
  };

  const error = subject.error ?? exams.error;
  const noFilterResult = Boolean(keyword || status);

  return (
    <div className="space-y-6">
      <PageHeader
        title={subject.data ? `Ca thi · ${subject.data.name}` : "Ca thi"}
        description="Quản lý bản nháp, lịch thi và học sinh được phân công trong môn đã chọn."
        action={
          <div className="flex flex-wrap gap-2">
            <Link to="/teacher/exams" className="inline-flex min-h-10 items-center rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold">
              Đổi môn
            </Link>
            <Link to="new" className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white">
              <Plus className="h-4 w-4" />
              Tạo ca thi
            </Link>
          </div>
        }
      />

      <FilterBar>
        <label className="flex flex-col min-w-60 flex-1 text-sm font-semibold">
          Tìm kiếm
          <Input
            className="mt-2"
            value={keyword}
            placeholder="Tên hoặc mã ca thi"
            onChange={(event) => updateSearchParam("keyword", event.target.value, true)}
          />
        </label>
        <label className="flex flex-col text-sm font-semibold">
          Trạng thái
          <Select
            className="mt-2 min-w-44"
            value={status}
            onChange={(event) => updateSearchParam("status", event.target.value, true)}
          >
            <option value="">Tất cả</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="SCHEDULED">Chờ bắt đầu</option>
            <option value="ACTIVE">Đang diễn ra</option>
            <option value="CLOSED">Đã kết thúc</option>
            <option value="CANCELLED">Đã hủy</option>
          </Select>
        </label>
      </FilterBar>

      {successMessage && <p role="status" className="rounded-lg bg-success/10 p-3 text-sm text-success">{successMessage}</p>}
      {(cancel.error || (schedule.error && !scheduleTarget)) && (
        <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">
          {schedule.error
            ? getScheduleErrorMessage(schedule.error)
            : getApiErrorMessage(cancel.error)}
        </p>
      )}

      <DataState
        loading={subject.isLoading || exams.isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={subject.isSuccess && exams.data?.content.length === 0}
        emptyMessage={noFilterResult ? "Không có ca thi phù hợp bộ lọc." : "Môn học này chưa có ca thi."}
        onRetry={() => {
          void subject.refetch();
          void exams.refetch();
        }}
      >
        <div className="space-y-3">
          {exams.data?.content.map((exam) => (
            <ExamListItem
              key={exam.id}
              exam={exam}
              expanded={expandedExamId === exam.id}
              onToggle={() => setExpandedExamId((current) => current === exam.id ? null : exam.id)}
              cancelling={cancel.isPending && cancel.variables === exam.id}
              onCancel={() => {
                schedule.reset();
                setSuccessMessage(null);
                setCancelTarget(exam);
              }}
              scheduling={schedule.isPending && schedule.variables === exam.id}
              scheduleDisabledReason={getScheduleDisabledReason(exam, now)}
              onSchedule={(target) => {
                if (getScheduleDisabledReason(target, Date.now())) return;
                cancel.reset();
                setSuccessMessage(null);
                schedule.reset();
                setScheduleTarget(target);
              }}
            />
          ))}
        </div>
      </DataState>

      {exams.data && (
        <Pagination
          page={exams.data.page}
          totalPages={exams.data.totalPages}
          onChange={(nextPage) => updateSearchParam("page", String(nextPage))}
        />
      )}

      <ConfirmDialog
        open={Boolean(cancelTarget)}
        title="Hủy ca thi?"
        description={cancelTarget ? `Ca thi “${cancelTarget.title}” sẽ chuyển sang trạng thái đã hủy và không thể tiếp tục chỉnh sửa.` : ""}
        confirmLabel="Hủy ca thi"
        tone="danger"
        loading={cancel.isPending}
        onOpenChange={(open) => {
          if (!open && !cancel.isPending) setCancelTarget(null);
        }}
        onConfirm={() => {
          if (cancelTarget) cancel.mutate(cancelTarget.id);
        }}
      />

      <ConfirmDialog
        open={Boolean(scheduleTarget)}
        title="Lên lịch ca thi?"
        description={scheduleTarget ? (
          <span className="space-y-2">
            <span className="block font-semibold text-ink">{scheduleTarget.title}</span>
            <span className="block">Bắt đầu: {new Date(scheduleTarget.startAt).toLocaleString("vi-VN")}</span>
            <span className="block">{scheduleTarget.questionCount} câu · {scheduleTarget.assignedCount} học sinh</span>
            <span className="block text-warning">
              Sau khi lên lịch, bạn không thể chỉnh sửa cấu hình, hủy ca thi hoặc thay đổi học sinh.
            </span>
            {schedule.error && (
              <span role="alert" className="block rounded-lg bg-danger/10 p-3 text-danger">
                {getScheduleErrorMessage(schedule.error)}
              </span>
            )}
          </span>
        ) : ""}
        confirmLabel="Xác nhận lên lịch"
        loading={schedule.isPending}
        onOpenChange={(open) => {
          if (!open && !schedule.isPending) setScheduleTarget(null);
        }}
        onConfirm={() => {
          if (scheduleTarget && !schedule.isPending) schedule.mutate(scheduleTarget.id);
        }}
      />
    </div>
  );
}

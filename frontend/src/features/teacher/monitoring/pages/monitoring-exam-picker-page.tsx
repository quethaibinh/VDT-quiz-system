import { useMemo, useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { Activity, ArrowRight, Clock, Search } from "lucide-react";
import { Link } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Input } from "@/components/ui/input";
import { StatusChip } from "@/components/ui/status-chip";
import { examKeys, listExams } from "@/features/teacher/exams/api/exam-repository";
import type { ExamStatus, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";
import { subjectListQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

const monitorableStatuses: ExamStatus[] = ["ACTIVE", "SCHEDULED"];

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function statusLabel(status: ExamStatus) {
  if (status === "ACTIVE") return "Đang diễn ra";
  if (status === "SCHEDULED") return "Sắp diễn ra";
  return status;
}

function statusTone(status: ExamStatus) {
  return status === "ACTIVE" ? "success" : "warning";
}

function sortMonitoringExams(a: ExamSummary, b: ExamSummary) {
  if (a.status !== b.status) return a.status === "ACTIVE" ? -1 : 1;
  return new Date(a.startAt).getTime() - new Date(b.startAt).getTime();
}

export function MonitoringExamPickerPage() {
  const [keyword, setKeyword] = useState("");
  const subjects = useQuery(subjectListQuery());
  const examQueries = useQueries({
    queries: (subjects.data ?? []).flatMap((subject) =>
      monitorableStatuses.map((status) => ({
        queryKey: examKeys.list(subject.id, { status, page: 0, size: 50 }),
        queryFn: () => listExams(subject.id, { status, page: 0, size: 50 }),
        enabled: subjects.isSuccess,
      })),
    ),
  });

  const exams = useMemo(() => {
    const byId = new Map<string, ExamSummary>();
    examQueries.forEach((query) => {
      query.data?.content.forEach((exam) => byId.set(exam.id, exam));
    });
    return Array.from(byId.values()).sort(sortMonitoringExams);
  }, [examQueries]);

  const filteredExams = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) return exams;
    return exams.filter((exam) =>
      `${exam.title} ${exam.code} ${exam.subjectName}`.toLowerCase().includes(normalized),
    );
  }, [exams, keyword]);

  const isLoading = subjects.isLoading || examQueries.some((query) => query.isLoading);
  const error = subjects.error ?? examQueries.find((query) => query.error)?.error;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Giám sát ca thi"
        description="Chọn ca thi đang diễn ra hoặc sắp diễn ra để mở bảng giám sát realtime."
      />

      <FilterBar>
        <label className="flex min-w-64 flex-1 flex-col text-sm font-semibold">
          Tìm kiếm
          <span className="relative mt-2">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
            <Input
              className="pl-9"
              value={keyword}
              placeholder="Tên, mã ca thi hoặc môn học"
              onChange={(event) => setKeyword(event.target.value)}
            />
          </span>
        </label>
      </FilterBar>

      <DataState
        loading={isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={!filteredExams.length}
        emptyMessage={keyword ? "Không có ca thi phù hợp." : "Chưa có ca thi đang hoặc sắp diễn ra để giám sát."}
        onRetry={() => {
          void subjects.refetch();
          examQueries.forEach((query) => void query.refetch());
        }}
      >
        <div className="grid gap-3">
          {filteredExams.map((exam) => (
            <article key={exam.id} className="rounded-lg border border-line bg-surface p-4 shadow-soft">
              <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div className="min-w-0 space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusChip tone={statusTone(exam.status)}>{statusLabel(exam.status)}</StatusChip>
                    <span className="text-xs font-semibold uppercase text-muted">{exam.subjectName}</span>
                  </div>
                  <div>
                    <h2 className="m-0 text-lg font-bold text-ink">{exam.title}</h2>
                    <p className="m-0 text-sm text-muted">{exam.code}</p>
                  </div>
                  <div className="flex flex-wrap gap-3 text-sm text-muted">
                    <span className="inline-flex items-center gap-1">
                      <Clock size={14} /> {formatDateTime(exam.startAt)}
                    </span>
                    <span>{exam.durationMinutes} phút</span>
                    <span>{exam.questionCount} câu</span>
                    <span>{exam.assignedCount} học sinh</span>
                  </div>
                </div>
                <Link
                  to={`/teacher/exams/${exam.id}/monitor`}
                  className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-hover"
                >
                  <Activity size={16} /> Mở giám sát <ArrowRight size={16} />
                </Link>
              </div>
            </article>
          ))}
        </div>
      </DataState>
    </div>
  );
}

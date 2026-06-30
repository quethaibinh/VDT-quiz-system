import { useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Award, BookOpen, ChevronRight, Clock, LockKeyhole, Medal } from "lucide-react";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusChip } from "@/components/ui/status-chip";
import { Button } from "@/components/ui/button";
import { buildStudentSubjectGroups, type StudentSubjectGroup } from "@/features/student/shared/student-subject-groups";
import { studentResultRepository } from "@/features/student/results/api/student-result-repository";
import type { StudentResultSummary, StudentResultVisibilityState } from "@/features/student/results/model/student-result-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

const stateLabels: Record<StudentResultVisibilityState, string> = {
  GRADING: "Đang chấm",
  READY: "Đã có điểm",
  RELEASED: "Đã có điểm",
  PENDING_REVIEW: "Chờ giáo viên duyệt",
  LOCKED_UNTIL_CLOSED: "Xem sau ca thi",
  GRADING_FAILED: "Lỗi chấm",
  CONFIG_MISSING: "Thiếu cấu hình",
};

export function StudentResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedSubjectId = searchParams.get("subjectId") ?? "";
  const query = useQuery({ queryKey: ["student", "results"], queryFn: () => studentResultRepository.listResults() });
  const results = query.data ?? [];
  const subjectGroups = useMemo(() => buildStudentSubjectGroups(results), [results]);
  const selectedSubject = subjectGroups.find((subject) => subject.subjectId === selectedSubjectId);
  const visibleResults = selectedSubjectId
    ? results.filter((result) => (result.subjectId ?? `unknown:${(result.subjectName || "Chưa rõ môn học").toLowerCase()}`) === selectedSubjectId)
    : results;

  return (
    <div className="space-y-6">
      <PageHeader
        title={selectedSubject ? `Kết quả · ${selectedSubject.subjectName}` : selectedSubjectId ? "Kết quả" : "Kết quả"}
        description={selectedSubjectId ? "Điểm và xếp hạng cá nhân của môn học đã chọn." : "Chọn môn học để xem kết quả các ca thi đã nộp."}
        action={selectedSubjectId ? <Button variant="secondary" onClick={() => setSearchParams({})}><ArrowLeft size={16} />Đổi môn</Button> : undefined}
      />
      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={query.isSuccess && (selectedSubjectId ? visibleResults.length === 0 : results.length === 0)}
        emptyMessage="Chưa có kết quả ca thi."
        onRetry={() => void query.refetch()}
      >
        {!selectedSubjectId ? (
          <SubjectPicker subjects={subjectGroups} onSelect={(subjectId) => setSearchParams({ subjectId })} />
        ) : (
          <div className="space-y-3">
            {visibleResults.map((result) => <ResultRow key={result.examId} result={result} />)}
          </div>
        )}
      </DataState>
    </div>
  );
}

function SubjectPicker({ subjects, onSelect }: { subjects: StudentSubjectGroup[]; onSelect: (subjectId: string) => void }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {subjects.map((subject) => (
        <button
          key={subject.subjectId}
          type="button"
          onClick={() => onSelect(subject.subjectId)}
          className="group flex min-h-32 items-center justify-between gap-4 rounded-xl border border-line bg-surface p-5 text-left shadow-soft transition hover:border-primary/40 hover:shadow-md"
        >
          <span className="flex min-w-0 items-center gap-3">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
              <BookOpen size={22} />
            </span>
            <span className="min-w-0">
              <span className="block truncate text-lg font-bold text-ink">{subject.subjectName}</span>
              <span className="mt-1 block text-sm font-medium text-muted">{subject.total} kết quả</span>
            </span>
          </span>
          <ChevronRight size={20} className="shrink-0 text-muted transition group-hover:text-primary" />
        </button>
      ))}
    </div>
  );
}

function ResultRow({ result }: { result: StudentResultSummary }) {
  const visible = result.visibilityState === "READY" || result.visibilityState === "RELEASED";
  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:grid-cols-[1fr_auto] md:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="m-0 text-xl">{result.title}</h2>
          <StatusChip tone={visible ? "success" : result.visibilityState === "GRADING_FAILED" ? "danger" : "warning"}>
            {stateLabels[result.visibilityState]}
          </StatusChip>
        </div>
        <p className="mb-0 mt-2 text-sm text-muted">
          {result.subjectName || "Ca thi"} · Nộp lúc {new Date(result.submittedAt).toLocaleString("vi-VN")}
        </p>
        {!visible && <p className="mb-0 mt-2 flex items-center gap-2 text-sm text-muted"><LockKeyhole size={14} /> {message(result)}</p>}
      </div>
      {visible ? (
        <div className="flex flex-wrap items-center justify-end gap-3">
          <div className="text-right">
            <strong className="block text-2xl text-primary">{result.score?.toFixed(2)} / {result.maxScore?.toFixed(2)}</strong>
            <small className="text-muted"><Medal size={12} className="inline" /> Hạng #{result.rank}</small>
          </div>
          <Link to={`/student/results/${result.examId}`}><Button><Award size={16} /> Xem</Button></Link>
        </div>
      ) : (
        <div className="flex items-center gap-2 text-sm font-semibold text-muted"><Clock size={16} /> Chưa mở</div>
      )}
    </article>
  );
}

function message(result: StudentResultSummary) {
  if (result.visibilityState === "LOCKED_UNTIL_CLOSED" && result.availableAt) {
    return `Mở sau ${new Date(result.availableAt).toLocaleString("vi-VN")}`;
  }
  if (result.visibilityState === "PENDING_REVIEW") return "Giáo viên chưa phát hành điểm.";
  if (result.visibilityState === "GRADING") return "Hệ thống đang chấm điểm.";
  return result.message;
}

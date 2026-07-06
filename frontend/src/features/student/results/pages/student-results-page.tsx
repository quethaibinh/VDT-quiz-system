import { useMemo } from "react";
import type { ReactNode } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Award, BookOpen, ChevronRight, ClipboardList, Clock, LockKeyhole, Medal, Zap } from "lucide-react";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusChip } from "@/components/ui/status-chip";
import { Button } from "@/components/ui/button";
import { buildStudentSubjectGroups, type StudentSubjectGroup } from "@/features/student/shared/student-subject-groups";
import { studentResultRepository } from "@/features/student/results/api/student-result-repository";
import {
  listStudentLiveQuizResults,
  studentLiveQuizResultIndexKeys,
} from "@/features/student/results/api/student-live-quiz-result-index-repository";
import type { StudentResultSummary, StudentResultVisibilityState } from "@/features/student/results/model/student-result-contracts";
import type { StudentLiveQuizResultSummary } from "@/features/student/live-quizzes/model/live-quiz-contracts";
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

type ResultCategory = "quiz" | "exam";

export function StudentResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedSubjectId = searchParams.get("subjectId") ?? "";
  const selectedType = (searchParams.get("type") ?? "") as ResultCategory | "";
  const examQuery = useQuery({ queryKey: ["student", "results"], queryFn: () => studentResultRepository.listResults() });
  const quizQuery = useQuery({
    queryKey: studentLiveQuizResultIndexKeys.list(),
    queryFn: () => listStudentLiveQuizResults(),
  });
  const examResults = useMemo(() => examQuery.data ?? [], [examQuery.data]);
  const quizResults = useMemo(() => quizQuery.data ?? [], [quizQuery.data]);
  const subjectGroups = useMemo(() => buildStudentSubjectGroups([...examResults, ...quizResults]), [examResults, quizResults]);
  const selectedSubject = subjectGroups.find((subject) => subject.subjectId === selectedSubjectId);
  const visibleExamResults = selectedSubjectId ? examResults.filter((result) => subjectKey(result) === selectedSubjectId) : examResults;
  const visibleQuizResults = selectedSubjectId ? quizResults.filter((result) => subjectKey(result) === selectedSubjectId) : quizResults;
  const error = examQuery.error ?? quizQuery.error;

  return (
    <div className="space-y-6">
      <PageHeader
        title={selectedSubject ? `Kết quả · ${selectedSubject.subjectName}` : "Kết quả"}
        description={selectedSubjectId ? "Chọn nhóm kết quả của môn học đã chọn." : "Chọn môn học để xem riêng kết quả quiz và kết quả ca thi."}
        action={selectedSubjectId ? <Button variant="secondary" onClick={() => setSearchParams({})}><ArrowLeft size={16} />Đổi môn</Button> : undefined}
      />
      <DataState
        loading={examQuery.isLoading || quizQuery.isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={examQuery.isSuccess && quizQuery.isSuccess && (selectedSubjectId ? visibleExamResults.length + visibleQuizResults.length === 0 : subjectGroups.length === 0)}
        emptyMessage="Chưa có kết quả."
        onRetry={() => { void examQuery.refetch(); void quizQuery.refetch(); }}
      >
        {!selectedSubjectId ? (
          <SubjectPicker subjects={subjectGroups} onSelect={(subjectId) => setSearchParams({ subjectId })} />
        ) : !selectedType ? (
          <CategoryPicker
            subjectId={selectedSubjectId}
            quizCount={visibleQuizResults.length}
            examCount={visibleExamResults.length}
            onSelect={(type) => setSearchParams({ subjectId: selectedSubjectId, type })}
          />
        ) : selectedType === "quiz" ? (
          <QuizResults results={visibleQuizResults} onBack={() => setSearchParams({ subjectId: selectedSubjectId })} />
        ) : (
          <ExamResults results={visibleExamResults} onBack={() => setSearchParams({ subjectId: selectedSubjectId })} />
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

function CategoryPicker({
  quizCount,
  examCount,
  onSelect,
}: {
  subjectId: string;
  quizCount: number;
  examCount: number;
  onSelect: (type: ResultCategory) => void;
}) {
  return (
    <div className="grid gap-4 md:grid-cols-2">
      <CategoryButton title="Kết quả quiz" count={quizCount} icon={<Zap size={24} />} onClick={() => onSelect("quiz")} />
      <CategoryButton title="Kết quả ca thi" count={examCount} icon={<ClipboardList size={24} />} onClick={() => onSelect("exam")} />
    </div>
  );
}

function CategoryButton({ title, count, icon, onClick }: { title: string; count: number; icon: ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="group grid min-h-36 gap-4 rounded-xl border border-line bg-surface p-5 text-left shadow-soft transition hover:border-primary/40 hover:shadow-md md:grid-cols-[auto_1fr_auto] md:items-center"
    >
      <span className="grid h-12 w-12 place-items-center rounded-lg bg-primary/10 text-primary">{icon}</span>
      <span>
        <span className="block text-2xl font-black text-ink">{title}</span>
        <span className="mt-2 block text-sm text-muted">{count} kết quả</span>
      </span>
      <ChevronRight size={22} className="text-muted transition group-hover:text-primary" />
    </button>
  );
}

function QuizResults({ results, onBack }: { results: StudentLiveQuizResultSummary[]; onBack: () => void }) {
  if (!results.length) {
    return <EmptyCategory message="Môn học này chưa có kết quả quiz." onBack={onBack} />;
  }
  return (
    <div className="space-y-3">
      <Button variant="secondary" onClick={onBack}><ArrowLeft size={16} />Đổi nhóm</Button>
      {results.map((result) => <QuizResultRow key={result.roomId} result={result} />)}
    </div>
  );
}

function ExamResults({ results, onBack }: { results: StudentResultSummary[]; onBack: () => void }) {
  if (!results.length) {
    return <EmptyCategory message="Môn học này chưa có kết quả ca thi." onBack={onBack} />;
  }
  return (
    <div className="space-y-3">
      <Button variant="secondary" onClick={onBack}><ArrowLeft size={16} />Đổi nhóm</Button>
      {results.map((result) => <ResultRow key={result.examId} result={result} />)}
    </div>
  );
}

function EmptyCategory({ message, onBack }: { message: string; onBack: () => void }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-6 shadow-soft">
      <p className="m-0 text-sm text-muted">{message}</p>
      <Button className="mt-4" variant="secondary" onClick={onBack}><ArrowLeft size={16} />Đổi nhóm</Button>
    </div>
  );
}

function QuizResultRow({ result }: { result: StudentLiveQuizResultSummary }) {
  return (
    <article className="grid gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft md:grid-cols-[1fr_auto] md:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="m-0 text-xl">{result.quizTitle}</h2>
          <StatusChip tone="success">Đã chốt</StatusChip>
        </div>
        <p className="mb-0 mt-2 text-sm text-muted">
          Quiz · Phòng {result.roomCode} · Chốt lúc {result.closedAt ? new Date(result.closedAt).toLocaleString("vi-VN") : "-"}
        </p>
      </div>
      <div className="flex flex-wrap items-center justify-end gap-3">
        <div className="text-right">
          <strong className="block text-2xl text-primary">{result.score.toFixed(2)} / {result.maxScore.toFixed(2)}</strong>
          <small className="text-muted"><Medal size={12} className="inline" /> Hạng #{result.finalRank}/{result.participantCount}</small>
        </div>
        <Link to={`/student/live-quizzes/${result.roomId}/result`}><Button><Award size={16} /> Xem</Button></Link>
      </div>
    </article>
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
          Ca thi · Nộp lúc {new Date(result.submittedAt).toLocaleString("vi-VN")}
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

function subjectKey(item: { subjectId: string | null; subjectName: string | null }) {
  const subjectName = item.subjectName?.trim() || "Chưa rõ môn học";
  return item.subjectId?.trim() || `unknown:${subjectName.toLowerCase()}`;
}

function message(result: StudentResultSummary) {
  if (result.visibilityState === "LOCKED_UNTIL_CLOSED" && result.availableAt) {
    return `Mở sau ${new Date(result.availableAt).toLocaleString("vi-VN")}`;
  }
  if (result.visibilityState === "PENDING_REVIEW") return "Giáo viên chưa phát hành điểm.";
  if (result.visibilityState === "GRADING") return "Hệ thống đang chấm điểm.";
  return result.message;
}

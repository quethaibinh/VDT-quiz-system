import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Search } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import {
  listLiveQuizzes,
  liveQuizKeys,
  prepareLiveQuiz,
} from "@/features/teacher/live-quizzes";
import { LiveQuizListItem } from "@/features/teacher/live-quizzes/components/live-quiz-list-item";
import type { LiveQuizStatus, LiveQuizSummary } from "@/features/teacher/live-quizzes";
import { subjectDetailQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

const PAGE_SIZE = 10;

export function LiveQuizListPage() {
  const { subjectId = "" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [expandedQuizId, setExpandedQuizId] = useState<string | null>(null);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const page = Math.max(0, Number(searchParams.get("page") ?? 0) || 0);
  const keyword = searchParams.get("keyword") ?? "";
  const status = (searchParams.get("status") ?? "") as LiveQuizStatus | "";
  const subject = useQuery(subjectDetailQuery(subjectId));
  const params = {
    page,
    size: PAGE_SIZE,
    keyword: keyword || undefined,
    status: status || undefined,
  };
  const quizzes = useQuery({
    queryKey: liveQuizKeys.list(subjectId, params),
    queryFn: () => listLiveQuizzes(subjectId, params),
    enabled: Boolean(subjectId) && subject.isSuccess,
  });
  const prepare = useMutation({
    mutationFn: (quiz: LiveQuizSummary) => prepareLiveQuiz(subjectId, quiz.id),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.subject(subjectId) });
      navigate(`/teacher/live-quizzes/${result.roomId}/room`, {
        state: {
          subjectId,
          quizId: result.quiz.id,
          quizTitle: result.quiz.title,
          roomCode: result.roomCode,
        },
      });
    },
    onError: async () => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.subject(subjectId) });
    },
  });

  const updateSearchParam = (key: string, value: string, resetPage = false) => {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (resetPage) next.delete("page");
    setSearchParams(next);
  };

  const error = subject.error ?? quizzes.error;
  const noFilterResult = Boolean(keyword || status);

  return (
    <div className="space-y-6">
      <PageHeader
        title={subject.data ? `Quiz · ${subject.data.name}` : "Quiz"}
        description="Tao ban nhap, prepare cau hoi va mo phong quiz truc tiep cho lop."
        action={
          <div className="flex flex-wrap gap-2">
            <Link to="/teacher/live-quizzes" className="inline-flex min-h-10 items-center rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold">
              Doi mon
            </Link>
            <Link to="new" className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white">
              <Plus className="h-4 w-4" />
              Tao quiz
            </Link>
          </div>
        }
      />

      <FilterBar>
        <label className="relative min-w-60 flex-1 text-sm font-semibold">
          Tim kiem
          <Search className="absolute left-3 top-10 h-5 w-5 text-muted" />
          <Input
            className="mt-2 pl-10"
            value={keyword}
            placeholder="Ten hoac ma quiz"
            onChange={(event) => updateSearchParam("keyword", event.target.value, true)}
          />
        </label>
        <label className="flex flex-col text-sm font-semibold">
          Trang thai
          <Select
            className="mt-2 min-w-44"
            value={status}
            onChange={(event) => updateSearchParam("status", event.target.value, true)}
          >
            <option value="">Tat ca</option>
            <option value="DRAFT">Ban nhap</option>
            <option value="PREPARED">Da chuan bi</option>
          </Select>
        </label>
      </FilterBar>

      {prepare.error && (
        <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">
          {getApiErrorMessage(prepare.error)}
        </p>
      )}

      <DataState
        loading={subject.isLoading || quizzes.isLoading}
        error={error ? getApiErrorMessage(error) : null}
        empty={subject.isSuccess && quizzes.data?.content.length === 0}
        emptyMessage={noFilterResult ? "Khong co quiz phu hop bo loc." : "Mon hoc nay chua co quiz."}
        onRetry={() => {
          void subject.refetch();
          void quizzes.refetch();
        }}
      >
        <div className="space-y-3">
          {quizzes.data?.content.map((quiz) => (
            <LiveQuizListItem
              key={quiz.id}
              quiz={quiz}
              expanded={expandedQuizId === quiz.id}
              preparing={prepare.isPending && prepare.variables?.id === quiz.id}
              onToggle={() => setExpandedQuizId((current) => current === quiz.id ? null : quiz.id)}
              onPrepare={(target) => prepare.mutate(target)}
            />
          ))}
        </div>
      </DataState>

      {quizzes.data && (
        <Pagination
          page={quizzes.data.page}
          totalPages={quizzes.data.totalPages}
          onChange={(nextPage) => updateSearchParam("page", String(nextPage))}
        />
      )}
    </div>
  );
}

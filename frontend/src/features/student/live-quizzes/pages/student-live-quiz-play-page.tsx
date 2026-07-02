import { useMutation, useQuery } from "@tanstack/react-query";
import { CheckCircle, Clock, Medal, Send, XCircle } from "lucide-react";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { Button } from "@/components/ui/button";
import { RuntimeClock } from "@/features/student/exams/lib/runtime-clock";
import {
  answerStudentLiveQuizQuestion,
  getStudentLiveQuizCurrentQuestion,
  getStudentLiveQuizState,
  studentLiveQuizKeys,
  type StudentLiveQuizAnswerResponse,
  type StudentLiveQuizCurrentQuestion,
} from "@/features/student/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentLiveQuizPlayPage() {
  const { roomId = "" } = useParams();
  const navigate = useNavigate();

  const state = useQuery({
    queryKey: studentLiveQuizKeys.state(roomId),
    queryFn: () => getStudentLiveQuizState(roomId),
    enabled: Boolean(roomId),
    refetchInterval: 5000,
  });
  const question = useQuery({
    queryKey: studentLiveQuizKeys.currentQuestion(roomId),
    queryFn: () => getStudentLiveQuizCurrentQuestion(roomId),
    enabled: Boolean(roomId) && state.data?.roomStatus === "STARTED" && state.data.participantStatus !== "FINISHED",
    retry: (failureCount, error) => (
      failureCount < 2 && getApiErrorMessage(error) === "CONCURRENT_OR_DUPLICATE_UPDATE"
    ),
    retryDelay: 350,
  });
  if (state.data?.roomStatus === "OPEN") {
    return <Navigate to={`/student/live-quizzes/${roomId}/lobby`} replace />;
  }

  const finished = state.data?.participantStatus === "FINISHED";

  return (
    <div className="min-h-screen bg-canvas">
      <DataState
        loading={state.isLoading || question.isLoading}
        error={state.error ? getApiErrorMessage(state.error) : question.error && !finished ? getApiErrorMessage(question.error) : null}
        onRetry={() => {
          void state.refetch();
          void question.refetch();
        }}
        empty={!state.data}
      >
        {state.data && (
          <>
            {finished ? (
              <main className="mx-auto grid min-h-[70vh] max-w-2xl place-items-center p-4">
                <section className="w-full rounded-xl border border-line bg-surface p-8 text-center shadow-soft">
                  <CheckCircle className="mx-auto text-success" size={44} />
                  <h1 className="mt-4 text-3xl font-black text-ink">Da hoan thanh</h1>
                  <p className="text-muted">Diem cua ban: <strong className="text-ink">{formatScore(state.data.totalScore)}</strong>{state.data.currentRank ? ` - Hang #${state.data.currentRank}` : ""}</p>
                  <Button className="mt-5" onClick={() => navigate("/student/live-quizzes", { replace: true })}>Ve man vao quiz</Button>
                </section>
              </main>
            ) : question.data ? (
              <QuestionSession
                key={question.data.questionId}
                roomId={roomId}
                question={question.data}
                state={state.data}
                onRefreshState={() => state.refetch()}
                onNext={() => question.refetch()}
              />
            ) : null}
          </>
        )}
      </DataState>
    </div>
  );
}

function QuestionSession({
  roomId,
  question,
  state,
  onRefreshState,
  onNext,
}: {
  roomId: string;
  question: StudentLiveQuizCurrentQuestion;
  state: NonNullable<Awaited<ReturnType<typeof getStudentLiveQuizState>>>;
  onRefreshState: () => Promise<unknown>;
  onNext: () => Promise<unknown>;
}) {
  const [selected, setSelected] = useState<string[]>([]);
  const [feedback, setFeedback] = useState<StudentLiveQuizAnswerResponse | null>(null);
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const answer = useMutation({
    mutationFn: () => answerStudentLiveQuizQuestion(roomId, {
      questionId: question.questionId,
      selectedOptionIds: selected,
    }),
    onSuccess: async (response) => {
      setFeedback(response);
      await onRefreshState();
    },
  });

  useEffect(() => {
    const clock = new RuntimeClock(question.serverTime);
    const update = () => setRemainingSeconds(clock.getRemainingSeconds(question.endsAt));
    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, [question.endsAt, question.serverTime]);

  useEffect(() => {
    if (remainingSeconds !== 0 || feedback || answer.isPending) return;
    void onNext();
    void onRefreshState();
  }, [answer.isPending, feedback, onNext, onRefreshState, remainingSeconds]);

  const currentScore = feedback?.totalScore ?? question.totalScore ?? state.totalScore ?? 0;
  const currentRank = question.currentRank ?? state.currentRank;
  const multipleChoice = question.type === "MULTI_CHOICE" || question.type === "MULTIPLE_CHOICE";
  const toggleOption = (optionId: string) => {
    if (!multipleChoice) {
      setSelected([optionId]);
      return;
    }
    setSelected((current) => (
      current.includes(optionId)
        ? current.filter((selectedId) => selectedId !== optionId)
        : [...current, optionId]
    ));
  };

  return (
    <>
      <header className="sticky top-0 z-20 border-b border-line bg-surface/95 px-4 py-3 shadow-soft backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="m-0 truncate text-sm font-bold text-ink">{state.quizTitle}</p>
            <p className="m-0 text-xs text-muted">{state.subjectName}</p>
          </div>
          <div className="flex flex-wrap items-center gap-2 text-sm font-bold">
            <Pill label="Diem" value={formatScore(currentScore)} />
            <Pill label="Hang" value={currentRank ? `#${currentRank}` : "..."} icon={<Medal size={14} />} />
            <Pill label="Tien do" value={`${state.answeredCount}/${state.totalQuestions}`} />
            <Pill label="Con lai" value={`${remainingSeconds}s`} icon={<Clock size={14} />} danger={remainingSeconds <= 5} />
          </div>
        </div>
      </header>

      <main className="mx-auto grid max-w-5xl gap-5 p-4 md:p-6">
        <QuestionCard
          question={question}
          multipleChoice={multipleChoice}
          selected={selected}
          disabled={Boolean(feedback) || answer.isPending || remainingSeconds <= 0}
          onSelect={toggleOption}
        />
        {feedback && <FeedbackPanel feedback={feedback} />}
        <div className="flex justify-end gap-3">
          {feedback?.nextQuestionAvailable && (
            <Button variant="secondary" onClick={() => onNext()}>
              Cau tiep theo
            </Button>
          )}
          {!feedback && (
            <Button disabled={selected.length === 0 || remainingSeconds <= 0} loading={answer.isPending} onClick={() => answer.mutate()}>
              <Send size={16} />
              Tra loi
            </Button>
          )}
        </div>
      </main>
    </>
  );
}

function QuestionCard({
  question,
  multipleChoice,
  selected,
  disabled,
  onSelect,
}: {
  question: StudentLiveQuizCurrentQuestion;
  multipleChoice: boolean;
  selected: string[];
  disabled: boolean;
  onSelect: (optionId: string) => void;
}) {
  return (
    <section className="rounded-xl border border-line bg-surface p-5 shadow-soft md:p-7">
      <p className="m-0 text-sm font-bold text-muted">Cau {question.questionPosition} / {question.totalQuestions}</p>
      <p className="m-0 mt-1 text-xs font-bold uppercase text-primary">
        {multipleChoice ? "Chon tat ca dap an dung" : "Chon 1 dap an"}
      </p>
      <div className="mt-4 whitespace-pre-wrap text-lg font-semibold leading-8 text-ink">{question.content}</div>
      <div className="mt-6 grid gap-3">
        {question.options.map((option) => {
          const active = selected.includes(option.optionId);
          return (
            <button
              key={option.optionId}
              type="button"
              disabled={disabled}
              onClick={() => onSelect(option.optionId)}
              className={`flex items-start gap-3 rounded-lg border p-4 text-left transition ${active ? "border-primary bg-primary/10 text-ink" : "border-line bg-canvas/30 text-muted hover:border-primary"} disabled:cursor-not-allowed disabled:opacity-80`}
            >
              <span className={`grid h-6 w-6 shrink-0 place-items-center border text-xs font-black ${multipleChoice ? "rounded" : "rounded-full"} ${active ? "border-primary bg-primary text-white" : "border-line bg-surface text-muted"}`}>
                {option.key}
              </span>
              <span className="leading-6">{option.content}</span>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function FeedbackPanel({ feedback }: { feedback: StudentLiveQuizAnswerResponse }) {
  const correct = feedback.answerStatus === "ANSWERED" && feedback.correct;
  const maxScore = feedback.maxScore;
  const scoreText = typeof maxScore === "number"
    ? `+${formatScore(feedback.scoreAwarded)}/${formatScore(maxScore)} diem`
    : `+${formatScore(feedback.scoreAwarded)} diem`;
  const responseTimeText = typeof feedback.responseTimeMs === "number"
    ? `Thoi gian tra loi: ${formatSeconds(feedback.responseTimeMs)}s`
    : null;
  return (
    <section role="status" className={`rounded-xl border p-4 shadow-soft ${correct ? "border-success/30 bg-success/10 text-success" : "border-danger/30 bg-danger/10 text-danger"}`}>
      <div className="flex items-center gap-2 font-bold">
        {correct ? <CheckCircle size={20} /> : <XCircle size={20} />}
        {feedback.answerStatus === "TIMEOUT" ? "Het gio cau hoi" : correct ? "Chinh xac" : "Chua dung"}
      </div>
      <p className="m-0 mt-1 text-sm">{scoreText} - Tong diem: {formatScore(feedback.totalScore)}</p>
      {responseTimeText && <p className="m-0 mt-1 text-xs font-semibold opacity-80">{responseTimeText}</p>}
    </section>
  );
}

function Pill({ label, value, icon, danger }: { label: string; value: string; icon?: ReactNode; danger?: boolean }) {
  return (
    <div className={`inline-flex min-h-10 items-center gap-2 rounded-lg border px-3 ${danger ? "border-danger bg-danger/10 text-danger" : "border-line bg-canvas text-ink"}`}>
      {icon}
      <span className="text-xs text-muted">{label}</span>
      <span>{value}</span>
    </div>
  );
}

function formatScore(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

function formatSeconds(milliseconds: number) {
  return (milliseconds / 1000).toLocaleString("vi-VN", { maximumFractionDigits: 1 });
}

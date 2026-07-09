import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle, Trophy, Users, XCircle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { Button } from "@/components/ui/button";
import { LiveQuizLeaderboard, type LiveQuizMetric } from "@/features/live-quizzes/components/live-quiz-leaderboard";
import { mapLeaderboardEntries } from "@/features/live-quizzes/lib/live-quiz-leaderboard-mappers";
import { useLiveQuizRealtime } from "@/features/live-quizzes/realtime/use-live-quiz-realtime";
import { RuntimeClock } from "@/features/student/exams/lib/runtime-clock";
import { LiveQuizQuestionStage } from "@/features/student/live-quizzes/components/live-quiz-question-stage";
import {
  answerStudentLiveQuizQuestion,
  getStudentLiveQuizFinalResult,
  getStudentLiveQuizCurrentQuestion,
  getStudentLiveQuizState,
  studentLiveQuizResultKeys,
  studentLiveQuizKeys,
  type StudentLiveQuizAnswerResponse,
  type StudentLiveQuizCurrentQuestion,
} from "@/features/student/live-quizzes";
import type { LiveQuizLeaderboardEntry } from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentLiveQuizPlayPage() {
  const { roomId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [leaderboardOverride, setLeaderboardOverride] = useState<{ roomId: string; entries: LiveQuizLeaderboardEntry[] } | null>(null);
  const [closedRealtimeRoomId, setClosedRealtimeRoomId] = useState<string | null>(null);

  const state = useQuery({
    queryKey: studentLiveQuizKeys.state(roomId),
    queryFn: () => getStudentLiveQuizState(roomId),
    enabled: Boolean(roomId),
    refetchInterval: (query) => query.state.data?.participantStatus === "FINISHED" ? false : 5000,
  });
  const effectiveRoomStatus = closedRealtimeRoomId === roomId || state.data?.roomStatus === "CLOSED" ? "CLOSED" : state.data?.roomStatus;
  const displayLeaderboard = state.data?.showLeaderboard
    ? leaderboardOverride?.roomId === roomId ? leaderboardOverride.entries : state.data.leaderboard
    : [];
  const question = useQuery({
    queryKey: studentLiveQuizKeys.currentQuestion(roomId),
    queryFn: () => getStudentLiveQuizCurrentQuestion(roomId),
    enabled: Boolean(roomId) && effectiveRoomStatus === "STARTED" && state.data?.participantStatus !== "FINISHED",
    retry: (failureCount, error) => (
      failureCount < 2 && getApiErrorMessage(error) === "CONCURRENT_OR_DUPLICATE_UPDATE"
    ),
    retryDelay: 350,
  });
  const finalResult = useQuery({
    queryKey: studentLiveQuizResultKeys.result(roomId),
    queryFn: () => getStudentLiveQuizFinalResult(roomId),
    // Hoc sinh lam xong chua co ket qua chinh thuc ngay.
    // Chi khi giao vien dong phong thi Result Service moi co ban rank/score da chot.
    enabled: Boolean(roomId) && state.data?.participantStatus === "FINISHED" && effectiveRoomStatus === "CLOSED",
    refetchInterval: (query) => query.state.data ? false : 3000,
    retry: true,
  });

  const realtimeTopics = useMemo(() => {
    const topics = [`/user/queue/live-quizzes/${roomId}`];
    if (state.data?.showLeaderboard) {
      topics.push(`/topic/live-quizzes/${roomId}/leaderboard`);
    }
    return topics;
  }, [roomId, state.data?.showLeaderboard]);

  const realtime = useLiveQuizRealtime({
    roomId,
    topics: realtimeTopics,
    enabled: Boolean(roomId && state.data),
    onReconnect: () => {
      void state.refetch();
      if (state.data?.participantStatus !== "FINISHED" && effectiveRoomStatus === "STARTED") {
        void question.refetch();
      }
    },
    onMessage: (message) => {
      if (message.leaderboard) {
        setLeaderboardOverride({ roomId, entries: message.leaderboard });
      }
      if (message.type === "ROOM_CLOSED" || message.roomStatus === "CLOSED") {
        setClosedRealtimeRoomId(roomId);
        void queryClient.invalidateQueries({ queryKey: studentLiveQuizKeys.state(roomId) });
        void finalResult.refetch();
      }
    },
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
              <main className="mx-auto w-full max-w-6xl p-4 sm:p-6">
                <section className="mx-auto mb-6 max-w-2xl rounded-xl border border-line bg-surface p-6 text-center shadow-soft sm:p-8">
                  <CheckCircle className="mx-auto text-success" size={44} />
                  <h1 className="mt-4 text-3xl font-black text-ink">{effectiveRoomStatus === "CLOSED" ? "Đang chốt kết quả" : "Đã hoàn thành"}</h1>
                  <p className="text-muted">
                    Điểm tạm thời: <strong className="text-ink">{formatScore(state.data.totalScore)}</strong>
                    {state.data.currentRank ? ` - Hạng tạm thời #${state.data.currentRank}` : ""}
                  </p>
                  <p className="text-sm text-muted">
                    {effectiveRoomStatus === "CLOSED"
                      ? "Giáo viên đã kết thúc quiz. Hệ thống đang lấy kết quả chính thức."
                      : "Vui lòng chờ giáo viên kết thúc quiz để chốt điểm và thứ hạng cuối cùng."}
                  </p>
                  {finalResult.data ? (
                    // Khi result-service da san sang, chuyen sang trang summary chot cho student.
                    <Button className="mt-5" onClick={() => navigate(`/student/live-quizzes/${roomId}/result`, { replace: true })}>Xem kết quả chính thức</Button>
                  ) : (
                    <Button className="mt-5" variant="secondary" onClick={() => {
                      void state.refetch();
                      void finalResult.refetch();
                    }}>Làm mới</Button>
                  )}
                </section>
                {state.data.showLeaderboard ? (
                  <LiveQuizLeaderboard
                    leaderboard={mapLeaderboardEntries(displayLeaderboard, state.data.totalQuestions, state.data.participantId)}
                    leaderboardBadge={realtime.realtimeActive ? "Live" : "Snapshot"}
                    leaderboardEmptyText="Chưa có dữ liệu xếp hạng."
                    leaderboardTitle="Bảng xếp hạng tạm thời"
                    metrics={studentLeaderboardMetrics(state.data)}
                    podiumTitle="Top học sinh hiện tại"
                    progressRows={mapLeaderboardEntries(displayLeaderboard, state.data.totalQuestions, state.data.participantId)}
                    progressTitle="Tiến độ học sinh"
                  />
                ) : null}
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

function studentLeaderboardMetrics(
  state: NonNullable<Awaited<ReturnType<typeof getStudentLiveQuizState>>>,
): LiveQuizMetric[] {
  return [
    { label: "Điểm tạm thời", value: formatScore(state.totalScore), icon: <Trophy size={17} /> },
    { label: "Hạng tạm thời", value: state.currentRank ? `#${state.currentRank}` : "...", icon: <Trophy size={17} /> },
    { label: "Đã trả lời", value: `${state.answeredCount}/${state.totalQuestions}` },
    { label: "Người tham gia", value: String(state.participantCount), icon: <Users size={17} /> },
  ];
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
    mutationFn: (selectedOptionIds: string[]) => answerStudentLiveQuizQuestion(roomId, {
      questionId: question.questionId,
      selectedOptionIds,
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

  useEffect(() => {
    if (!feedback) return;
    const timer = window.setTimeout(() => {
      void onRefreshState();
      if (!feedback.finished) {
        void onNext();
      }
    }, 1000);
    return () => window.clearTimeout(timer);
  }, [feedback, onNext, onRefreshState]);

  const currentScore = feedback?.totalScore ?? question.totalScore ?? state.totalScore ?? 0;
  const currentRank = question.currentRank ?? state.currentRank;
  const multipleChoice = question.type === "MULTI_CHOICE" || question.type === "MULTIPLE_CHOICE";
  const locked = Boolean(feedback) || answer.isPending || remainingSeconds <= 0;
  const optionFeedbackById = useMemo(() => Object.fromEntries(
    (feedback?.selectedOptionResults ?? []).map((result) => [result.optionId, result.result]),
  ), [feedback]);
  const toggleOption = (optionId: string) => {
    if (locked) return;
    if (!multipleChoice) {
      setSelected([optionId]);
      answer.mutate([optionId]);
      return;
    }
    setSelected((current) => (
      current.includes(optionId)
        ? current.filter((selectedId) => selectedId !== optionId)
        : [...current, optionId]
    ));
  };

  return (
    <LiveQuizQuestionStage
      quizTitle={state.quizTitle}
      subjectName={state.subjectName}
      questionPosition={question.questionPosition}
      totalQuestions={question.totalQuestions}
      content={question.content}
      options={question.options}
      isMultipleChoice={multipleChoice}
      selectedOptionIds={selected}
      optionFeedbackById={optionFeedbackById}
      feedbackActive={Boolean(feedback)}
      disabled={locked}
      submitting={answer.isPending}
      remainingSeconds={remainingSeconds}
      scoreText={formatScore(currentScore)}
      rankText={currentRank ? `#${currentRank}` : "..."}
      onOptionSelect={toggleOption}
      onSubmit={() => answer.mutate(selected)}
      canSubmit={!feedback && selected.length > 0 && remainingSeconds > 0}
      feedback={feedback ? <FeedbackPanel feedback={feedback} /> : null}
    />
  );
}


function FeedbackPanel({ feedback }: { feedback: StudentLiveQuizAnswerResponse }) {
  const correct = feedback.answerStatus === "ANSWERED" && feedback.correct;
  const maxScore = feedback.maxScore;
  const scoreText = typeof maxScore === "number"
    ? `+${formatScore(feedback.scoreAwarded)}/${formatScore(maxScore)} điểm`
    : `+${formatScore(feedback.scoreAwarded)} điểm`;
  const responseTimeText = typeof feedback.responseTimeMs === "number"
    ? `Thời gian trả lời: ${formatSeconds(feedback.responseTimeMs)}s`
    : null;
  return (
    <section role="status" className={`rounded-xl border p-4 shadow-soft ${correct ? "border-success/30 bg-success/10 text-success" : "border-danger/30 bg-danger/10 text-danger"}`}>
      <div className="flex items-center gap-2 font-bold">
        {correct ? <CheckCircle size={20} /> : <XCircle size={20} />}
        {feedback.answerStatus === "TIMEOUT" ? "Hết giờ câu hỏi" : correct ? "Chính xác" : "Chưa đúng"}
      </div>
      <p className="m-0 mt-1 text-sm">{scoreText} - Tổng điểm: {formatScore(feedback.totalScore)}</p>
      {responseTimeText && <p className="m-0 mt-1 text-xs font-semibold opacity-80">{responseTimeText}</p>}
    </section>
  );
}

function formatScore(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

function formatSeconds(milliseconds: number) {
  return (milliseconds / 1000).toLocaleString("vi-VN", { maximumFractionDigits: 1 });
}

import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AlertCircle, ArrowLeft, ChevronLeft, ChevronRight, Maximize2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataState } from "@/components/shared/data-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { studentRuntimeRepository } from "../api/student-runtime-repository";
import { studentExamRepository } from "../api/student-exam-repository";
import { initializeAnswers } from "../lib/runtime-answer-state";
import { RuntimeClock } from "../lib/runtime-clock";
import { ExamRuntimeHeader } from "../components/exam-runtime-header";
import { QuestionView } from "../components/question-view";
import { QuestionNavigator } from "../components/question-navigator";
import { SessionLockedOverlay } from "../components/session-locked-overlay";
import { useStudentProctoring } from "../hooks/use-student-proctoring";
import type { StudentAnswer, StudentPaperResponse, SubmitResponse } from "../model/student-exam-contracts";

const submitKeyPrefix = "student-exam-submit-idempotency";

function createIdempotencyKey() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function getSessionSubmitIdempotencyKey(sessionId: string) {
  const storageKey = `${submitKeyPrefix}:${sessionId}`;
  const existing = window.sessionStorage.getItem(storageKey);
  if (existing) return existing;

  const next = createIdempotencyKey();
  window.sessionStorage.setItem(storageKey, next);
  return next;
}

// Man hinh lam bai thi tap trung cua hoc sinh
export function StudentExamRuntimePage() {
  const { examId = "" } = useParams();
  const navigate = useNavigate();
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, StudentAnswer>>({});
  const [dirtyQuestionIds, setDirtyQuestionIds] = useState<Set<string>>(() => new Set());
  const [answerVersions, setAnswerVersions] = useState<Record<string, number>>({});
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const [isTimeUp, setIsTimeUp] = useState(false);
  const [saveState, setSaveState] = useState<"idle" | "saving" | "error">("idle");
  const [lastSavedAt, setLastSavedAt] = useState<string | null>(null);
  const [submittedResponse, setSubmittedResponse] = useState<SubmitResponse | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitConfirmOpen, setSubmitConfirmOpen] = useState(false);
  const [fullscreenActive, setFullscreenActive] = useState(() => Boolean(document.fullscreenElement));
  const [fullscreenError, setFullscreenError] = useState<string | null>(null);

  const clientSeqRef = useRef(1);
  const serverTimeRef = useRef("");
  const answerVersionsRef = useRef<Record<string, number>>({});
  const answersRef = useRef(answers);
  const dirtyQuestionIdsRef = useRef(dirtyQuestionIds);
  const remainingSecondsRef = useRef(remainingSeconds);
  const isTimeUpRef = useRef(isTimeUp);
  const paperDataRef = useRef<StudentPaperResponse | undefined>(undefined);
  const flushTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const saveInFlightRef = useRef(false);
  const flushAfterCurrentRef = useRef(false);
  const submitInFlightRef = useRef(false);
  const submittedRef = useRef(false);
  const timeUpSubmitStartedRef = useRef(false);

  const submitMutation = useMutation({
    mutationFn: (input: { sessionId: string; idempotencyKey: string; clientSeq: number; finalAnswers: StudentAnswer[] }) =>
      studentRuntimeRepository.submitStudentSession(input.sessionId, {
        idempotencyKey: input.idempotencyKey,
        clientSeq: input.clientSeq,
        finalAnswers: input.finalAnswers,
      }),
  });

  const examQuery = useQuery({
    queryKey: ["student", "exam-runtime-detail", examId],
    queryFn: () => studentExamRepository.getStudentExam(examId),
    enabled: Boolean(examId),
  });

  const paperQuery = useQuery({
    queryKey: ["student", "paper-runtime", examId],
    queryFn: async () => {
      const joinResult = await studentRuntimeRepository.joinStudentExam(examId);
      serverTimeRef.current = joinResult.serverTime;

      if (joinResult.status === "IN_PROGRESS") {
        return studentRuntimeRepository.resumeStudentSession(joinResult.sessionId);
      }

      navigate(`/student/exams/${examId}/lobby`, { replace: true });
      throw new Error("EXAM_NOT_STARTED");
    },
    enabled: Boolean(examId),
    retry: false,
  });

  useEffect(() => {
    answerVersionsRef.current = answerVersions;
  }, [answerVersions]);

  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  useEffect(() => {
    dirtyQuestionIdsRef.current = dirtyQuestionIds;
  }, [dirtyQuestionIds]);

  useEffect(() => {
    remainingSecondsRef.current = remainingSeconds;
  }, [remainingSeconds]);

  useEffect(() => {
    isTimeUpRef.current = isTimeUp;
  }, [isTimeUp]);

  useEffect(() => {
    submittedRef.current = Boolean(submittedResponse);
  }, [submittedResponse]);

  useEffect(() => {
    paperDataRef.current = paperQuery.data;
  }, [paperQuery.data]);

  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (dirtyQuestionIdsRef.current.size > 0) {
        e.preventDefault();
        e.returnValue = "Bạn có bài làm chưa được lưu. Bạn có chắc chắn muốn rời đi?";
        return e.returnValue;
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, []);

  useEffect(() => {
    if (!paperQuery.data) return;

    // Existing page state is derived from the loaded runtime paper.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setAnswers(initializeAnswers(paperQuery.data.questions, paperQuery.data.answers));
    setDirtyQuestionIds(new Set());
    setAnswerVersions({});
    setSaveState("idle");
    setSubmittedResponse(null);
    setSubmitError(null);
    setSubmitConfirmOpen(false);
    submitInFlightRef.current = false;
    submittedRef.current = false;
    timeUpSubmitStartedRef.current = false;
    flushAfterCurrentRef.current = false;
    saveInFlightRef.current = false;
    if (flushTimerRef.current) {
      clearTimeout(flushTimerRef.current);
      flushTimerRef.current = null;
    }
  }, [paperQuery.data]);

  useEffect(() => {
    if (!paperQuery.data || !serverTimeRef.current) return;

    const clock = new RuntimeClock(serverTimeRef.current);
    const updateTimer = () => {
      const remaining = clock.getRemainingSeconds(paperQuery.data.serverDeadlineAt);
      setRemainingSeconds(remaining);
      if (remaining <= 0) setIsTimeUp(true);
    };

    updateTimer();
    const timer = setInterval(updateTimer, 1000);
    return () => clearInterval(timer);
  }, [paperQuery.data]);

  useEffect(() => {
    const handleFullscreenChange = () => {
      const active = Boolean(document.fullscreenElement);
      setFullscreenActive(active);
      if (active) setFullscreenError(null);
    };
    handleFullscreenChange();
    document.addEventListener("fullscreenchange", handleFullscreenChange);
    return () => document.removeEventListener("fullscreenchange", handleFullscreenChange);
  }, []);

  useEffect(() => () => {
    if (flushTimerRef.current) {
      clearTimeout(flushTimerRef.current);
      flushTimerRef.current = null;
    }
  }, []);

  const resolveAutosaveDelay = () => {
    const remaining = remainingSecondsRef.current;
    if (remaining <= 10) return Math.floor(Math.random() * 100);
    if (remaining <= 30) return 300;
    return Math.floor(Math.random() * 301) + 500;
  };

  const scheduleAutosaveFlush = (delay = resolveAutosaveDelay()) => {
    if (!paperDataRef.current || isTimeUpRef.current || submittedRef.current) return;
    if (flushTimerRef.current) clearTimeout(flushTimerRef.current);
    flushTimerRef.current = setTimeout(() => {
      flushTimerRef.current = null;
      void flushAutosave();
    }, delay);
  };

  const flushAutosave = async (options?: { allowAfterTimeUp?: boolean }) => {
    const paper = paperDataRef.current;
    if (!paper || submittedRef.current || (isTimeUpRef.current && !options?.allowAfterTimeUp)) return true;

    if (saveInFlightRef.current) {
      flushAfterCurrentRef.current = true;
      return true;
    }

    const idsToSave = Array.from(dirtyQuestionIdsRef.current);
    const answersList = idsToSave
      .map((id) => answersRef.current[id])
      .filter((answer): answer is StudentAnswer => Boolean(answer));
    if (answersList.length === 0) return true;

    const versionsAtSave = idsToSave.reduce<Record<string, number>>((acc, id) => {
      acc[id] = answerVersionsRef.current[id] ?? 0;
      return acc;
    }, {});

    saveInFlightRef.current = true;
    flushAfterCurrentRef.current = false;
    setSaveState("saving");
    let failed = false;

    try {
      const res = await studentRuntimeRepository.autosaveStudentAnswers(paper.sessionId, {
        clientSeq: clientSeqRef.current,
        answers: answersList,
      });

      setSaveState("idle");
      setLastSavedAt(res.lastAutosaveAt);
      setDirtyQuestionIds((prev) => {
        const next = new Set(prev);
        idsToSave.forEach((id) => {
          if ((answerVersionsRef.current[id] ?? 0) === versionsAtSave[id]) {
            next.delete(id);
          }
        });
        return next;
      });
    } catch (error) {
      failed = true;
      console.error("Lưu đáp án thất bại", error);
      setSaveState("error");
    } finally {
      saveInFlightRef.current = false;
      if (flushAfterCurrentRef.current || dirtyQuestionIdsRef.current.size > 0) {
        scheduleAutosaveFlush(failed ? 1000 : resolveAutosaveDelay());
      }
    }

    return !failed;
  };

  const markDirty = (questionId: string) => {
    if (submittedRef.current) return;
    const nextSeq = clientSeqRef.current + 1;
    clientSeqRef.current = nextSeq;
    setAnswerVersions((prev) => ({ ...prev, [questionId]: nextSeq }));
    setDirtyQuestionIds((prev) => new Set(prev).add(questionId));
    scheduleAutosaveFlush();
  };

  const handleAnswerChange = (questionId: string, selectedOptionIds: string[]) => {
    if (!fullscreenActive || isTimeUp || submitInFlightRef.current || submittedRef.current) return;

    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        selectedOptionIds,
      },
    }));
    markDirty(questionId);
  };

  const handleMarkForReviewToggle = (questionId: string) => {
    if (!fullscreenActive || isTimeUp || submitInFlightRef.current || submittedRef.current) return;

    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        markedForReview: !prev[questionId].markedForReview,
      },
    }));
    markDirty(questionId);
  };

  const getCurrentAnswersList = (paper: StudentPaperResponse) =>
    paper.questions
      .map((question) => answersRef.current[question.questionId])
      .filter((answer): answer is StudentAnswer => Boolean(answer));

  const { locked: proctoringLocked, lastAlert, connectionStatus: proctoringConnectionStatus } = useStudentProctoring({
    examId,
    sessionId: paperQuery.data?.sessionId,
    enabled: Boolean(paperQuery.data && paperQuery.data.status === "IN_PROGRESS" && !submittedResponse),
  });

  const submitCurrentSession = async (options?: { allowWithoutFullscreen?: boolean }) => {
    const paper = paperDataRef.current;
    if (!paper || (!options?.allowWithoutFullscreen && !fullscreenActive) || proctoringLocked || submitInFlightRef.current || submittedRef.current) return;

    submitInFlightRef.current = true;
    setSubmitError(null);
    if (flushTimerRef.current) {
      clearTimeout(flushTimerRef.current);
      flushTimerRef.current = null;
    }

    try {
      await flushAutosave({ allowAfterTimeUp: true });
      const response = await submitMutation.mutateAsync({
        sessionId: paper.sessionId,
        idempotencyKey: getSessionSubmitIdempotencyKey(paper.sessionId),
        clientSeq: clientSeqRef.current,
        finalAnswers: getCurrentAnswersList(paper),
      });
      setSubmittedResponse(response);
      setDirtyQuestionIds(new Set());
      setSaveState("idle");
      submittedRef.current = true;
    } catch (error) {
      console.error("Nộp bài thất bại", error);
      setSubmitError("Không thể nộp bài lúc này. Hệ thống sẽ tiếp tục thử lại nếu hết giờ.");
    } finally {
      submitInFlightRef.current = false;
    }
  };

  useEffect(() => {
    if (!isTimeUp || timeUpSubmitStartedRef.current || submittedRef.current) return;
    timeUpSubmitStartedRef.current = true;
    void submitCurrentSession({ allowWithoutFullscreen: true });
    // submitCurrentSession reads refs so this effect fires only on the time-up edge.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isTimeUp]);

  const requestRuntimeFullscreen = async () => {
    setFullscreenError(null);
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen();
      }
      setFullscreenActive(Boolean(document.fullscreenElement));
    } catch {
      setFullscreenError("Khong the bat che do toan man hinh. Vui long cho phep fullscreen de tiep tuc lam bai.");
    }
  };

  const isLoading = examQuery.isLoading || paperQuery.isLoading;
  const error = examQuery.error ?? paperQuery.error;
  const questions = paperQuery.data?.questions ?? [];
  const currentQuestion = questions[currentIndex];
  const currentAnswer = currentQuestion ? answers[currentQuestion.questionId] : null;
  const isSubmitPending = submitMutation.isPending;
  const isSubmitted = Boolean(submittedResponse);
  const isLocked = proctoringLocked || paperQuery.data?.status === "LOCKED";
  const fullscreenRequired = Boolean(paperQuery.data && !fullscreenActive && !isSubmitted && !isLocked);
  const editingDisabled = fullscreenRequired || isLocked || isTimeUp || isSubmitPending || isSubmitted;

  return (
    <div className="flex min-h-screen flex-col bg-canvas">
      <DataState
        loading={isLoading}
        error={error ? "Không thể tải đề thi. Vui lòng thử lại." : null}
        onRetry={() => {
          examQuery.refetch();
          paperQuery.refetch();
        }}
      >
        {examQuery.data && paperQuery.data && (
          <>
            {isLocked && <SessionLockedOverlay alert={lastAlert} onExit={() => navigate("/student/exams", { replace: true })} />}
            {fullscreenRequired && (
              <div className="fixed inset-0 z-40 flex items-center justify-center bg-ink/70 p-4">
                <div className="w-[min(92vw,480px)] space-y-4 rounded-2xl border border-line bg-surface p-6 text-center shadow-soft">
                  <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-warning/10 text-warning">
                    <Maximize2 size={24} />
                  </div>
                  <div className="space-y-2">
                    <h2 className="m-0 text-xl font-bold text-ink">Can quay lai che do toan man hinh</h2>
                    <p className="m-0 text-sm leading-6 text-muted">
                      Bai thi dang duoc giam sat. Ban phai o che do toan man hinh de tiep tuc thao tac.
                    </p>
                  </div>
                  {fullscreenError && (
                    <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">{fullscreenError}</p>
                  )}
                  <Button onClick={() => void requestRuntimeFullscreen()}>
                    <Maximize2 size={16} /> Quay lai fullscreen
                  </Button>
                </div>
              </div>
            )}
            <ExamRuntimeHeader
              title={examQuery.data.title}
              remainingSeconds={remainingSeconds}
              saveState={saveState}
              lastSavedAt={lastSavedAt}
              submitPending={isSubmitPending}
              submitted={isSubmitted}
              submitDisabled={fullscreenRequired || isTimeUp || isLocked}
              onSubmit={() => setSubmitConfirmOpen(true)}
            />
            <ConfirmDialog
              open={submitConfirmOpen}
              onOpenChange={setSubmitConfirmOpen}
              title="Nộp bài?"
              description="Sau khi xác nhận nộp bài, bạn sẽ không thể sửa đáp án."
              confirmLabel="Xác nhận nộp"
              loading={isSubmitPending}
              onConfirm={() => {
                setSubmitConfirmOpen(false);
                void submitCurrentSession();
              }}
            />
            {submittedResponse && (
              <FinalStateDialog
                title="Đã ghi nhận bài nộp"
                description={`Bài nộp đã được tiếp nhận lúc ${new Date(submittedResponse.submittedAt).toLocaleTimeString("vi-VN")}.`}
                actionLabel="Về màn ca thi"
                onAction={() => navigate("/student/exams", { replace: true })}
              />
            )}

            <div className="border-b border-line bg-surface px-4 py-2 text-center text-xs font-semibold text-muted">
              Giam sat realtime: {proctoringConnectionStatus === "connected"
                ? "Da ket noi"
                : proctoringConnectionStatus === "error"
                  ? "Loi ket noi"
                  : proctoringConnectionStatus === "reconnecting"
                    ? "Dang noi lai"
                    : "Dang ket noi"}
            </div>

            {(isTimeUp || submitError || submittedResponse) && (
              <div className={`flex items-center justify-center gap-2 border-b p-4 text-center font-semibold ${
                submittedResponse
                  ? "border-success/20 bg-success/10 text-success"
                  : "border-danger/20 bg-danger/10 text-danger"
              }`}>
                <AlertCircle size={18} />
                <span>
                  {submittedResponse
                    ? `Bài nộp đã được tiếp nhận lúc ${new Date(submittedResponse.submittedAt).toLocaleTimeString("vi-VN")}.`
                    : submitError ?? "Hết giờ làm bài. Hệ thống đang gửi bài nộp tốt nhất có thể."}
                </span>
              </div>
            )}

            <main className="mx-auto grid w-full max-w-7xl flex-1 grid-cols-1 items-start gap-6 p-4 md:p-6 lg:grid-cols-3">
              <div className="space-y-4 lg:col-span-2">
                {currentQuestion && currentAnswer && (
                  <QuestionView
                    question={currentQuestion}
                    index={currentIndex}
                    answer={currentAnswer}
                    disabled={editingDisabled}
                    onAnswerChange={(optionIds) => handleAnswerChange(currentQuestion.questionId, optionIds)}
                    onMarkForReviewToggle={() => handleMarkForReviewToggle(currentQuestion.questionId)}
                  />
                )}

                <div className="flex items-center justify-between rounded-xl border border-line bg-surface p-4 shadow-soft">
                  <Button
                    variant="secondary"
                    disabled={fullscreenRequired || currentIndex === 0}
                    onClick={() => setCurrentIndex((prev) => prev - 1)}
                  >
                    <ChevronLeft size={16} /> Câu trước
                  </Button>
                  <span className="text-sm font-semibold text-muted">
                    {currentIndex + 1} / {questions.length}
                  </span>
                  <Button
                    variant="secondary"
                    disabled={fullscreenRequired || currentIndex === questions.length - 1}
                    onClick={() => setCurrentIndex((prev) => prev + 1)}
                  >
                    Câu tiếp theo <ChevronRight size={16} />
                  </Button>
                </div>
              </div>

              <aside className="space-y-6">
                <QuestionNavigator
                  questions={questions}
                  answers={answers}
                  currentIndex={currentIndex}
                  onSelect={(index) => {
                    if (!fullscreenRequired) setCurrentIndex(index);
                  }}
                />

                <div className="space-y-3 rounded-2xl border border-line bg-surface p-5 shadow-soft">
                  <p className="text-xs leading-relaxed text-muted">
                    Nếu gặp sự cố đường truyền, hãy giữ nguyên màn hình để hệ thống tự động thử lại.
                    Không tự ý reload tab nếu chưa có trạng thái đã lưu.
                  </p>
                  <div className="hidden">
                    <Button variant="ghost" className="flex w-full items-center gap-1 text-xs text-muted">
                      <ArrowLeft size={12} /> Quay lại danh sách ca thi
                    </Button>
                  </div>
                </div>
              </aside>
            </main>
          </>
        )}
      </DataState>
    </div>
  );
}

function FinalStateDialog({
  title,
  description,
  actionLabel,
  onAction,
}: {
  title: string;
  description: string;
  actionLabel: string;
  onAction: () => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/45 p-4">
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="runtime-final-state-title"
        className="w-full max-w-md rounded-2xl border border-line bg-surface p-6 text-center shadow-soft"
      >
        <h2 id="runtime-final-state-title" className="m-0 text-2xl font-bold text-ink">{title}</h2>
        <p className="mx-auto mt-3 max-w-sm text-sm leading-6 text-muted">{description}</p>
        <Button className="mt-5" onClick={onAction}>{actionLabel}</Button>
      </section>
    </div>
  );
}

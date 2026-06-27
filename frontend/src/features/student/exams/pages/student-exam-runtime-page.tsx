import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { AlertCircle, ArrowLeft, ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataState } from "@/components/shared/data-state";
import { studentRuntimeRepository } from "../api/student-runtime-repository";
import { studentExamRepository } from "../api/student-exam-repository";
import { initializeAnswers } from "../lib/runtime-answer-state";
import { RuntimeClock } from "../lib/runtime-clock";
import { ExamRuntimeHeader } from "../components/exam-runtime-header";
import { QuestionView } from "../components/question-view";
import { QuestionNavigator } from "../components/question-navigator";
import type { StudentAnswer, StudentPaperResponse } from "../model/student-exam-contracts";

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

  const clientSeqRef = useRef(Date.now());
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
      if (joinResult.status === "CREATED" && joinResult.canStart) {
        return studentRuntimeRepository.startStudentExam(examId);
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

    setAnswers(initializeAnswers(paperQuery.data.questions, paperQuery.data.answers));
    setDirtyQuestionIds(new Set());
    setAnswerVersions({});
    setSaveState("idle");
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
    if (!paperDataRef.current || isTimeUpRef.current) return;
    if (flushTimerRef.current) clearTimeout(flushTimerRef.current);
    flushTimerRef.current = setTimeout(() => {
      flushTimerRef.current = null;
      void flushAutosave();
    }, delay);
  };

  const flushAutosave = async () => {
    const paper = paperDataRef.current;
    if (!paper || isTimeUpRef.current) return;

    if (saveInFlightRef.current) {
      flushAfterCurrentRef.current = true;
      return;
    }

    const idsToSave = Array.from(dirtyQuestionIdsRef.current);
    const answersList = idsToSave.map((id) => answersRef.current[id]).filter(Boolean);
    if (answersList.length === 0) return;

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
  };

  const markDirty = (questionId: string) => {
    const nextSeq = clientSeqRef.current + 1;
    clientSeqRef.current = nextSeq;
    setAnswerVersions((prev) => ({ ...prev, [questionId]: nextSeq }));
    setDirtyQuestionIds((prev) => new Set(prev).add(questionId));
    scheduleAutosaveFlush();
  };

  const handleAnswerChange = (questionId: string, selectedOptionIds: string[]) => {
    if (isTimeUp) return;

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
    if (isTimeUp) return;

    setAnswers((prev) => ({
      ...prev,
      [questionId]: {
        ...prev[questionId],
        markedForReview: !prev[questionId].markedForReview,
      },
    }));
    markDirty(questionId);
  };

  const isLoading = examQuery.isLoading || paperQuery.isLoading;
  const error = examQuery.error ?? paperQuery.error;
  const questions = paperQuery.data?.questions ?? [];
  const currentQuestion = questions[currentIndex];
  const currentAnswer = currentQuestion ? answers[currentQuestion.questionId] : null;

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
            <ExamRuntimeHeader
              title={examQuery.data.title}
              remainingSeconds={remainingSeconds}
              saveState={saveState}
              lastSavedAt={lastSavedAt}
            />

            {isTimeUp && (
              <div className="flex items-center justify-center gap-2 border-b border-danger/20 bg-danger/10 p-4 text-center font-semibold text-danger">
                <AlertCircle size={18} />
                <span>Hết giờ làm bài - tính năng nộp bài tự động/thủ công chưa được hỗ trợ.</span>
              </div>
            )}

            <main className="mx-auto grid w-full max-w-7xl flex-1 grid-cols-1 items-start gap-6 p-4 md:p-6 lg:grid-cols-3">
              <div className="space-y-4 lg:col-span-2">
                {currentQuestion && currentAnswer && (
                  <QuestionView
                    question={currentQuestion}
                    index={currentIndex}
                    answer={currentAnswer}
                    disabled={isTimeUp}
                    onAnswerChange={(optionIds) => handleAnswerChange(currentQuestion.questionId, optionIds)}
                    onMarkForReviewToggle={() => handleMarkForReviewToggle(currentQuestion.questionId)}
                  />
                )}

                <div className="flex items-center justify-between rounded-xl border border-line bg-surface p-4 shadow-soft">
                  <Button
                    variant="secondary"
                    disabled={currentIndex === 0}
                    onClick={() => setCurrentIndex((prev) => prev - 1)}
                  >
                    <ChevronLeft size={16} /> Câu trước
                  </Button>
                  <span className="text-sm font-semibold text-muted">
                    {currentIndex + 1} / {questions.length}
                  </span>
                  <Button
                    variant="secondary"
                    disabled={currentIndex === questions.length - 1}
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
                  onSelect={(index) => setCurrentIndex(index)}
                />

                <div className="space-y-3 rounded-2xl border border-line bg-surface p-5 shadow-soft">
                  <p className="text-xs leading-relaxed text-muted">
                    Nếu gặp sự cố đường truyền, hãy giữ nguyên màn hình để hệ thống tự động thử lại.
                    Không tự ý reload tab nếu chưa có trạng thái đã lưu.
                  </p>
                  <Link to="/student/exams" className="block">
                    <Button variant="ghost" className="flex w-full items-center gap-1 text-xs text-muted">
                      <ArrowLeft size={12} /> Quay lại danh sách ca thi
                    </Button>
                  </Link>
                </div>
              </aside>
            </main>
          </>
        )}
      </DataState>
    </div>
  );
}

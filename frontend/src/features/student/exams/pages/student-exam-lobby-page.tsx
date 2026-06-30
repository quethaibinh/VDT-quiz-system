import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowRight, CalendarDays, Clock, HelpCircle, Maximize2, Play, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { getApiErrorMessage } from "@/lib/http/api-error";
import { studentExamRepository } from "../api/student-exam-repository";
import { studentRuntimeRepository } from "../api/student-runtime-repository";
import { ExamLobbyState } from "../components/exam-lobby-state";

function formatDuration(totalSeconds: number) {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [h > 0 ? String(h).padStart(2, "0") : null, String(m).padStart(2, "0"), String(s).padStart(2, "0")]
    .filter(Boolean)
    .join(":");
}

function formatDateTime(isoString: string) {
  return new Date(isoString).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function StudentExamLobbyPage() {
  const { examId = "" } = useParams();
  const navigate = useNavigate();
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const [consentOpen, setConsentOpen] = useState(false);
  const [consentAction, setConsentAction] = useState<"start" | "continue">("start");
  const [fullscreenError, setFullscreenError] = useState<string | null>(null);
  const [fullscreenPending, setFullscreenPending] = useState(false);

  const examQuery = useQuery({
    queryKey: ["student", "exam-detail", examId],
    queryFn: () => studentExamRepository.getStudentExam(examId),
    enabled: Boolean(examId),
  });

  const joinQuery = useQuery({
    queryKey: ["student", "exam-join", examId],
    queryFn: () => studentRuntimeRepository.joinStudentExam(examId),
    enabled: Boolean(examId),
    retry: false,
  });

  const startMutation = useMutation({
    mutationFn: () => studentRuntimeRepository.startStudentExam(examId),
    onSuccess: () => navigate(`/student/exams/${examId}/session`),
  });

  useEffect(() => {
    if (!joinQuery.data) return;
    const timer = setTimeout(() => setRemainingSeconds(joinQuery.data.remainingSecondsToStart), 0);
    return () => clearTimeout(timer);
  }, [joinQuery.data]);

  useEffect(() => {
    if (remainingSeconds === null || remainingSeconds <= 0) return;
    const interval = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(interval);
          void joinQuery.refetch();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [remainingSeconds, joinQuery]);

  const openConsent = (action: "start" | "continue") => {
    setConsentAction(action);
    setFullscreenError(null);
    startMutation.reset();
    setConsentOpen(true);
  };

  const requestExamFullscreen = async () => {
    if (document.fullscreenElement) return;
    if (!document.documentElement.requestFullscreen) {
      throw new Error("FULLSCREEN_UNSUPPORTED");
    }
    await document.documentElement.requestFullscreen();
    if (!document.fullscreenElement) {
      throw new Error("FULLSCREEN_NOT_ACTIVE");
    }
  };

  const acceptMonitoringAndEnter = async () => {
    setFullscreenError(null);
    setFullscreenPending(true);
    try {
      await requestExamFullscreen();
      if (consentAction === "start") {
        startMutation.mutate();
      } else {
        navigate(`/student/exams/${examId}/session`);
      }
    } catch {
      setFullscreenError("Ban can cho phep che do toan man hinh de bat dau lam bai.");
    } finally {
      setFullscreenPending(false);
    }
  };

  if (joinQuery.isError) {
    const errorMsg = getApiErrorMessage(joinQuery.error);
    const businessErrors = [
      "EXAM_RUNTIME_NOT_READY",
      "STUDENT_NOT_ASSIGNED",
      "EXAM_JOIN_NOT_OPEN",
      "EXAM_LATE_JOIN_CLOSED",
      "EXAM_ALREADY_ENDED",
      "EXAM_ALREADY_LOCKED",
      "SESSION_NOT_FOUND",
      "UNAUTHORIZED_SESSION",
    ];

    if (businessErrors.includes(errorMsg)) {
      return <ExamLobbyState errorCode={errorMsg} />;
    }
  }

  const isLoading = examQuery.isLoading || joinQuery.isLoading;
  const error = examQuery.error ?? joinQuery.error;

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 md:p-6">
      <PageHeader
        title="Thong tin ca thi va phong cho"
        description="Doc ky quy che va xac nhan giam sat truoc khi bat dau lam bai."
      />

      <DataState
        loading={isLoading}
        error={error ? "Khong the truy cap phong cho thi. Vui long thu lai." : null}
        onRetry={() => {
          void examQuery.refetch();
          void joinQuery.refetch();
        }}
      >
        {examQuery.data && joinQuery.data && (
          <div className="grid gap-6 md:grid-cols-3">
            <section className="space-y-6 md:col-span-2">
              <div className="space-y-4 rounded-2xl border border-line bg-surface p-5 shadow-soft md:p-6">
                <div className="space-y-1">
                  <span className="text-xs font-semibold uppercase tracking-wider text-muted">
                    {examQuery.data.subjectName}
                  </span>
                  <h1 className="m-0 text-2xl font-bold text-ink">{examQuery.data.title}</h1>
                </div>

                {examQuery.data.description && (
                  <div className="rounded-xl border border-line bg-canvas p-4 text-sm text-muted">
                    <p className="mb-1 font-semibold text-ink">Mo ta ca thi:</p>
                    <p className="whitespace-pre-line">{examQuery.data.description}</p>
                  </div>
                )}

                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <Clock className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">Thoi gian</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.durationMinutes} phut</span>
                  </div>
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <HelpCircle className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">So cau hoi</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.questionCount} cau</span>
                  </div>
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <CalendarDays className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">Ma ca thi</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.code}</span>
                  </div>
                </div>

                <div className="space-y-2 border-t border-line pt-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-muted">Quy che phong thi:</p>
                  <ul className="list-disc space-y-1.5 pl-5 text-sm text-muted">
                    <li>Khong roi khoi tab thi khi dang lam bai.</li>
                    <li>Phai chap nhan giam sat va vao che do toan man hinh truoc khi bat dau.</li>
                    <li>Copy, paste, menu chuot phai va thoat fullscreen se bi chan hoac ghi nhan.</li>
                    <li>Khi het gio hoac bi khoa, man hinh lam bai se khoa thao tac.</li>
                  </ul>
                </div>
              </div>
            </section>

            <aside className="space-y-6">
              <div className="flex min-h-[300px] flex-col justify-between rounded-2xl border border-line bg-surface p-6 text-center shadow-soft">
                <div className="space-y-4">
                  <h3 className="text-lg font-bold text-ink">Trang thai phong cho</h3>
                  <div className="h-px w-full bg-line" />
                </div>

                {joinQuery.data.status === "CREATED" && !joinQuery.data.canStart && (
                  <div className="my-6 space-y-3">
                    <div className="font-mono text-4xl font-extrabold tracking-wider text-primary">
                      {remainingSeconds !== null ? formatDuration(remainingSeconds) : "00:00"}
                    </div>
                    <p className="text-sm text-muted">Vui long cho den khi ca thi bat dau.</p>
                    <p className="text-xs text-muted">Bat dau luc: {formatDateTime(examQuery.data.startAt)}</p>
                  </div>
                )}

                {joinQuery.data.status === "CREATED" && joinQuery.data.canStart && (
                  <div className="my-6 space-y-4">
                    <div className="mb-2 inline-flex rounded-full bg-success/10 p-3 text-success">
                      <Play size={28} />
                    </div>
                    <p className="text-sm font-semibold text-ink">Ca thi da bat dau.</p>
                    <p className="text-xs text-muted">Ban can chap nhan giam sat de tai de va lam bai.</p>
                  </div>
                )}

                {joinQuery.data.status === "IN_PROGRESS" && (
                  <div className="my-6 space-y-4">
                    <div className="mb-2 inline-flex rounded-full bg-warning/10 p-3 text-warning">
                      <Clock size={28} />
                    </div>
                    <p className="text-sm font-semibold text-ink">Ban co bai thi dang lam.</p>
                    <p className="text-xs text-muted">He thong se khoi phuc de va dap an da luu.</p>
                  </div>
                )}

                <div className="space-y-3">
                  {joinQuery.data.status === "CREATED" && (
                    <Button
                      variant="primary"
                      className="h-11 w-full"
                      disabled={!joinQuery.data.canStart || startMutation.isPending || fullscreenPending}
                      loading={startMutation.isPending || fullscreenPending}
                      onClick={() => openConsent("start")}
                    >
                      Bat dau lam bai <ArrowRight size={16} />
                    </Button>
                  )}

                  {joinQuery.data.status === "IN_PROGRESS" && (
                    <Button
                      variant="primary"
                      className="h-11 w-full"
                      disabled={fullscreenPending}
                      loading={fullscreenPending}
                      onClick={() => openConsent("continue")}
                    >
                      Tiep tuc lam bai <ArrowRight size={16} />
                    </Button>
                  )}
                </div>
              </div>
            </aside>
          </div>
        )}
      </DataState>

      <ConfirmDialog
        open={consentOpen}
        title="Xac nhan giam sat ca thi"
        description={(
          <span className="space-y-3">
            <span className="flex items-start gap-2 rounded-lg border border-line bg-canvas p-3 text-ink">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <span>
                Bai thi nay duoc giam sat realtime. He thong se ghi nhan viec roi tab, mat focus,
                thoat fullscreen, copy, paste va mo menu chuot phai.
              </span>
            </span>
            <span className="flex items-start gap-2 rounded-lg border border-line bg-canvas p-3 text-ink">
              <Maximize2 className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <span>Ban phai cho phep che do toan man hinh truoc khi vao lam bai.</span>
            </span>
            {fullscreenError && (
              <span role="alert" className="block rounded-lg bg-danger/10 p-3 text-danger">
                {fullscreenError}
              </span>
            )}
            {startMutation.error && (
              <span role="alert" className="block rounded-lg bg-danger/10 p-3 text-danger">
                {getApiErrorMessage(startMutation.error)}
              </span>
            )}
          </span>
        )}
        confirmLabel="Toi hieu va chap nhan"
        loading={fullscreenPending || startMutation.isPending}
        onOpenChange={(open) => {
          setConsentOpen(open);
          if (!open) {
            setFullscreenError(null);
            startMutation.reset();
          }
        }}
        onConfirm={() => {
          void acceptMonitoringAndEnter();
        }}
      />
    </div>
  );
}

import { useEffect, useMemo, useState } from "react";
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
import { RuntimeClock } from "../lib/runtime-clock";

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
    retry: (failureCount, error) => {
      return getApiErrorMessage(error) === "EXAM_NOT_STARTED" && failureCount < 5;
    },
    retryDelay: 1000,
  });

  const clock = useMemo(() => {
    if (!joinQuery.data) return null;
    return new RuntimeClock(joinQuery.data.serverTime);
  }, [joinQuery.data]);

  useEffect(() => {
    if (!joinQuery.data || !clock) return;

    const update = () => {
      const remaining = clock.getRemainingSeconds(joinQuery.data.startAt);
      setRemainingSeconds(remaining);
    };

    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [joinQuery.data, clock]);

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
      setFullscreenError("Bạn cần cho phép chế độ toàn màn hình để bắt đầu làm bài.");
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

  const canStartFrontend = joinQuery.data?.canStart || (remainingSeconds !== null && remainingSeconds <= 0);
  const isLoading = examQuery.isLoading || joinQuery.isLoading;
  const error = examQuery.error ?? joinQuery.error;

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 md:p-6">
      <PageHeader
        title="Thông tin ca thi và phòng chờ"
        description="Đọc kỹ quy chế và xác nhận giám sát trước khi bắt đầu làm bài."
      />

      <DataState
        loading={isLoading}
        error={error ? "Không thể truy cập phòng chờ thi. Vui lòng thử lại." : null}
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
                    <p className="mb-1 font-semibold text-ink">Mô tả ca thi:</p>
                    <p className="whitespace-pre-line">{examQuery.data.description}</p>
                  </div>
                )}

                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <Clock className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">Thời gian</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.durationMinutes} phút</span>
                  </div>
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <HelpCircle className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">Số câu hỏi</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.questionCount} câu</span>
                  </div>
                  <div className="space-y-1 rounded-xl border border-line bg-canvas p-3 text-center">
                    <CalendarDays className="mx-auto text-primary" size={20} />
                    <span className="block text-xs text-muted">Mã ca thi</span>
                    <span className="block text-sm font-bold text-ink">{examQuery.data.code}</span>
                  </div>
                </div>

                <div className="space-y-2 border-t border-line pt-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-muted">Quy chế phòng thi:</p>
                  <ul className="list-disc space-y-1.5 pl-5 text-sm text-muted">
                    <li>Không rời khỏi tab thi khi đang làm bài.</li>
                    <li>Phải chấp nhận giám sát và vào chế độ toàn màn hình trước khi bắt đầu.</li>
                    <li>Copy, paste, menu chuột phải và thoát fullscreen sẽ bị chặn hoặc ghi nhận.</li>
                    <li>Khi hết giờ hoặc bị khóa, màn hình làm bài sẽ khóa thao tác.</li>
                  </ul>
                </div>
              </div>
            </section>

            <aside className="space-y-6">
              <div className="flex min-h-[300px] flex-col justify-between rounded-2xl border border-line bg-surface p-6 text-center shadow-soft">
                <div className="space-y-4">
                  <h3 className="text-lg font-bold text-ink">Trạng thái phòng chờ</h3>
                  <div className="h-px w-full bg-line" />
                </div>

                {joinQuery.data.status === "CREATED" && !canStartFrontend && (
                  <div className="my-6 space-y-3">
                    <div className="font-mono text-4xl font-extrabold tracking-wider text-primary">
                      {remainingSeconds !== null ? formatDuration(remainingSeconds) : "00:00"}
                    </div>
                    <p className="text-sm text-muted">Vui lòng chờ đến khi ca thi bắt đầu.</p>
                    <p className="text-xs text-muted">Bắt đầu lúc: {formatDateTime(examQuery.data.startAt)}</p>
                  </div>
                )}

                {joinQuery.data.status === "CREATED" && canStartFrontend && (
                  <div className="my-6 space-y-4">
                    <div className="mb-2 inline-flex rounded-full bg-success/10 p-3 text-success">
                      <Play size={28} />
                    </div>
                    <p className="text-sm font-semibold text-ink">Ca thi đã bắt đầu.</p>
                    <p className="text-xs text-muted">Bạn cần chấp nhận giám sát để tải đề và làm bài.</p>
                  </div>
                )}

                {joinQuery.data.status === "IN_PROGRESS" && (
                  <div className="my-6 space-y-4">
                    <div className="mb-2 inline-flex rounded-full bg-warning/10 p-3 text-warning">
                      <Clock size={28} />
                    </div>
                    <p className="text-sm font-semibold text-ink">Bạn có bài thi đang làm.</p>
                    <p className="text-xs text-muted">Hệ thống sẽ khôi phục đề và đáp án đã lưu.</p>
                  </div>
                )}

                <div className="space-y-3">
                  {joinQuery.data.status === "CREATED" && (
                    <Button
                      variant="primary"
                      className="h-11 w-full"
                      disabled={!canStartFrontend || startMutation.isPending || fullscreenPending}
                      loading={startMutation.isPending || fullscreenPending}
                      onClick={() => openConsent("start")}
                    >
                      Bắt đầu làm bài <ArrowRight size={16} />
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
                      Tiếp tục làm bài <ArrowRight size={16} />
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
        title="Xác nhận giám sát ca thi"
        description={(
          <span className="space-y-3">
            <span className="flex items-start gap-2 rounded-lg border border-line bg-canvas p-3 text-ink">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <span>
                Bài thi này được giám sát realtime. Hệ thống sẽ ghi nhận việc rời tab, mất focus,
                thoát fullscreen, copy, paste và mở menu chuột phải.
              </span>
            </span>
            <span className="flex items-start gap-2 rounded-lg border border-line bg-canvas p-3 text-ink">
              <Maximize2 className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
              <span>Bạn phải cho phép chế độ toàn màn hình trước khi vào làm bài.</span>
            </span>
            {fullscreenError && (
              <span role="alert" className="block rounded-lg bg-danger/10 p-3 text-danger">
                {fullscreenError}
              </span>
            )}
            {startMutation.error && getApiErrorMessage(startMutation.error) !== "EXAM_NOT_STARTED" && (
              <span role="alert" className="block rounded-lg bg-danger/10 p-3 text-danger">
                {getApiErrorMessage(startMutation.error)}
              </span>
            )}
          </span>
        )}
        confirmLabel="Tôi hiểu và chấp nhận"
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

import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowRight, CalendarDays, Clock, HelpCircle, Play } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { getApiErrorMessage } from "@/lib/http/api-error";
import { studentExamRepository } from "../api/student-exam-repository";
import { studentRuntimeRepository } from "../api/student-runtime-repository";
import { ExamLobbyState } from "../components/exam-lobby-state";

// Dinh dang thoi gian dem nguoc dang HH:MM:SS
function formatDuration(totalSeconds: number) {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [h > 0 ? String(h).padStart(2, "0") : null, String(m).padStart(2, "0"), String(s).padStart(2, "0")]
    .filter(Boolean)
    .join(":");
}

// Format ngay gio de hoc sinh de doc
function formatDateTime(isoString: string) {
  return new Date(isoString).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

// Trang phong cho thi cua hoc sinh
export function StudentExamLobbyPage() {
  const { examId = "" } = useParams();
  const navigate = useNavigate();
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);

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
    if (joinQuery.data) setRemainingSeconds(joinQuery.data.remainingSecondsToStart);
  }, [joinQuery.data]);

  useEffect(() => {
    if (remainingSeconds === null || remainingSeconds <= 0) return;
    const interval = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(interval);
          joinQuery.refetch();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [remainingSeconds, joinQuery]);

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
        title="Thông tin ca thi và phòng chờ"
        description="Đọc kỹ quy chế và chờ đến thời gian bắt đầu làm bài."
      />

      <DataState
        loading={isLoading}
        error={error ? "Không thể truy cập phòng chờ thi. Vui lòng thử lại." : null}
        onRetry={() => {
          examQuery.refetch();
          joinQuery.refetch();
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
                    <li>Mọi thay đổi đáp án sẽ được tự động lưu.</li>
                    <li>Đồng hồ làm bài được tính theo thời gian máy chủ.</li>
                    <li>Khi hết giờ, màn hình làm bài sẽ khóa thao tác.</li>
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

                {joinQuery.data.status === "CREATED" && !joinQuery.data.canStart && (
                  <div className="my-6 space-y-3">
                    <div className="font-mono text-4xl font-extrabold tracking-wider text-primary">
                      {remainingSeconds !== null ? formatDuration(remainingSeconds) : "00:00"}
                    </div>
                    <p className="text-sm text-muted">Vui lòng chờ đến khi ca thi bắt đầu.</p>
                    <p className="text-xs text-muted">Bắt đầu lúc: {formatDateTime(examQuery.data.startAt)}</p>
                  </div>
                )}

                {joinQuery.data.status === "CREATED" && joinQuery.data.canStart && (
                  <div className="my-6 space-y-4">
                    <div className="mb-2 inline-flex rounded-full bg-success/10 p-3 text-success">
                      <Play size={28} />
                    </div>
                    <p className="text-sm font-semibold text-ink">Ca thi đã bắt đầu.</p>
                    <p className="text-xs text-muted">Bấm nút bên dưới để tải đề và làm bài.</p>
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
                      disabled={!joinQuery.data.canStart || startMutation.isPending}
                      loading={startMutation.isPending}
                      onClick={() => startMutation.mutate()}
                    >
                      Bắt đầu làm bài <ArrowRight size={16} />
                    </Button>
                  )}

                  {joinQuery.data.status === "IN_PROGRESS" && (
                    <Button
                      variant="primary"
                      className="h-11 w-full"
                      onClick={() => navigate(`/student/exams/${examId}/session`)}
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
    </div>
  );
}

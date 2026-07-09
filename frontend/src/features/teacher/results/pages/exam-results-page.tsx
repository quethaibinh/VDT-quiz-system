import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as Dialog from "@radix-ui/react-dialog";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Link, useLocation, useParams } from "react-router-dom";
import { ChevronDown, ChevronUp, Download, Eye, FileClock, Send, SlidersHorizontal, X } from "lucide-react";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { getMonitor } from "@/features/teacher/monitoring/api/monitor-repository";
import type { MonitorEvent, MonitorParticipant } from "@/features/teacher/monitoring/model/monitor-contracts";
import {
  formatEventSummary,
  formatRelativeTime,
  getEventLabel,
  getSeverityTone,
  getStatusLabel,
} from "@/features/teacher/monitoring/model/monitor-format";
import {
  adjustTeacherResultScore,
  exportTeacherExamResults,
  getTeacherExamResults,
  getTeacherResultDetail,
  publishTeacherExamResults,
  publishTeacherResult,
} from "@/features/teacher/results/api/result-repository";
import type {
  ResultReviewStatus,
  ResultVisibilityState,
  TeacherResultDetail,
} from "@/features/teacher/results/model/result-contracts";
import {
  buildGradebookView,
  getStudentMonitorEvents,
  type GradebookResultRow,
} from "@/features/teacher/results/model/result-monitoring";
import { getApiErrorMessage } from "@/lib/http/api-error";

interface ResultLocationState {
  backTo?: string;
  examTitle?: string;
}

type TeacherResultAnswer = TeacherResultDetail["answers"][number];

const reviewLabels: Record<ResultReviewStatus, string> = {
  PENDING_REVIEW: "Chờ duyệt",
  RELEASED: "Đã phát hành",
};

const visibilityLabels: Record<ResultVisibilityState, string> = {
  GRADING: "Đang chấm",
  READY: "Có thể xem",
  PENDING_REVIEW: "Chờ duyệt",
  RELEASED: "Đã phát hành",
  LOCKED_UNTIL_CLOSED: "Sau ca thi",
  GRADING_FAILED: "Lỗi chấm",
  CONFIG_MISSING: "Thiếu cấu hình",
};

const answerLabels: Record<TeacherResultAnswer["answerState"], string> = {
  CORRECT: "Đúng",
  WRONG: "Sai",
  BLANK: "Bỏ chọn",
};

const answerTones: Record<TeacherResultAnswer["answerState"], "success" | "danger" | "warning"> = {
  CORRECT: "success",
  WRONG: "danger",
  BLANK: "warning",
};

const outcomeLabels: Record<GradebookResultRow["outcome"], string> = {
  GRADED: "",
  LOCKED: "Bị khóa",
  NOT_JOIN: "Chưa tham gia",
};

export function ExamResultsPage() {
  const { examId = "" } = useParams();
  const location = useLocation();
  const state = location.state as ResultLocationState | null;
  const queryClient = useQueryClient();
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);
  const [selectedLogStudentId, setSelectedLogStudentId] = useState<string | null>(null);
  const [expandedQuestionId, setExpandedQuestionId] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ["teacher", "results", examId],
    queryFn: () => getTeacherExamResults(examId, { size: 100 }),
    enabled: Boolean(examId),
  });

  const detailQuery = useQuery({
    queryKey: ["teacher", "results", examId, selectedResultId],
    queryFn: () => getTeacherResultDetail(examId, selectedResultId!),
    enabled: Boolean(examId && selectedResultId),
  });

  const monitorQuery = useQuery({
    queryKey: ["teacher", "results", examId, "monitor"],
    queryFn: () => getMonitor(examId),
    enabled: Boolean(examId),
    retry: false,
  });

  const invalidate = () => {
    void queryClient.invalidateQueries({ queryKey: ["teacher", "results", examId] });
  };

  const publishOne = useMutation({
    mutationFn: (resultId: string) => publishTeacherResult(examId, resultId),
    onSuccess: invalidate,
  });

  const publishAll = useMutation({
    mutationFn: () => publishTeacherExamResults(examId),
    onSuccess: invalidate,
  });

  const adjustScore = useMutation({
    mutationFn: (input: { resultId: string; adjustedScore: number; reason: string }) =>
      adjustTeacherResultScore(examId, input.resultId, { adjustedScore: input.adjustedScore, reason: input.reason }),
    onSuccess: () => {
      invalidate();
      if (selectedResultId) {
        void queryClient.invalidateQueries({ queryKey: ["teacher", "results", examId, selectedResultId] });
      }
    },
  });

  const exportMutation = useMutation({
    mutationFn: () => exportTeacherExamResults(examId),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `exam-results-${examId}.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
    },
  });

  const handleAdjust = (row: GradebookResultRow) => {
    if (!row.resultId || !row.canAdjust) return;
    const scoreValue = window.prompt("Nhập điểm mới", String(row.effectiveScore));
    if (scoreValue == null) return;
    const reason = window.prompt("Lý do điều chỉnh");
    if (!reason) return;
    const adjustedScore = Number(scoreValue);
    if (Number.isNaN(adjustedScore)) return;
    adjustScore.mutate({ resultId: row.resultId, adjustedScore, reason });
  };

  const results = query.data?.students.content ?? [];
  const monitorParticipants = monitorQuery.data?.participants ?? [];
  const monitorEvents = monitorQuery.data?.events ?? [];
  const gradebook = buildGradebookView(results, monitorParticipants, query.data?.distribution);
  const displayRows = gradebook.rows;
  const selectedLogParticipant = selectedLogStudentId
    ? monitorParticipants.find((participant) => participant.studentId === selectedLogStudentId)
    : undefined;
  const selectedLogEvents = selectedLogStudentId
    ? getStudentMonitorEvents(monitorEvents, selectedLogStudentId)
    : [];

  return (
    <div className="space-y-6">
      <PageHeader
        title={query.data?.title || state?.examTitle || "Kết quả ca thi"}
        description={query.data?.subjectName || "Danh sách điểm và trạng thái phát hành."}
        action={
          <div className="flex flex-wrap gap-2">
            <Link to={state?.backTo || "/teacher/results"}><Button variant="secondary">Quay lại</Button></Link>
            <Button variant="secondary" loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
              <Download size={16} /> Xuất Excel
            </Button>
            <Button loading={publishAll.isPending} onClick={() => publishAll.mutate()}>
              <Send size={16} /> Phát hành tất cả
            </Button>
          </div>
        }
      />
      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={query.isSuccess && displayRows.length === 0}
        onRetry={() => void query.refetch()}
      >
        {query.data && (
          <>
            <div className="grid gap-3 md:grid-cols-5">
              <Metric label="Đã chấm" value={`${gradebook.gradedCount}/${gradebook.participantCount}`} />
              <Metric label="Đã phát hành" value={String(gradebook.releasedCount)} />
              <Metric label="Chờ duyệt" value={String(gradebook.pendingReviewCount)} />
              <Metric label="Trung bình" value={gradebook.average.toFixed(1)} />
              <Metric label="Cao nhất" value={gradebook.highest.toFixed(1)} />
            </div>

            <section className="grid gap-5 xl:grid-cols-[360px_1fr]">
              <div className="h-80 rounded-xl border border-line bg-surface p-5 shadow-soft">
                <h2 className="mt-0 text-xl">Phân bố điểm</h2>
                <ResponsiveContainer width="100%" height="85%">
                  <BarChart data={gradebook.distribution}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="range" />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="count" fill="#c2652a" />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="max-h-[min(64vh,560px)] overflow-auto rounded-xl border border-line bg-surface shadow-soft">
                <table className="w-full min-w-[920px] border-collapse text-sm">
                  <thead className="sticky top-0 z-10 bg-ink/5 text-left text-xs uppercase text-muted shadow-[0_1px_0_var(--color-line)]">
                    <tr>
                      <th className="p-3">Học sinh</th>
                      <th className="p-3">Điểm</th>
                      <th className="p-3">Đúng/Sai/Trống</th>
                      <th className="p-3">Hạng</th>
                      <th className="p-3">Trạng thái</th>
                      <th className="p-3">Nộp lúc</th>
                      <th className="p-3 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {displayRows.map((row) => (
                      <tr key={row.resultId ?? row.studentId} className="border-t border-line">
                        <td className="p-3">
                          <strong>{row.studentName}</strong>
                          <small className="block text-muted">{row.studentCode || row.studentId}</small>
                        </td>
                        <td className="p-3">
                          <strong className="text-primary">{row.effectiveScore.toFixed(2)}</strong>
                          <span className="text-muted"> / {row.maxScore.toFixed(2)}</span>
                          {row.adjustedScore != null && <small className="block text-warning">Đã sửa từ {row.originalScore.toFixed(2)}</small>}
                        </td>
                        <td className="p-3">{row.correctCount}/{row.wrongCount}/{row.blankCount}</td>
                        <td className="p-3">#{row.rank}</td>
                        <td className="p-3">
                          <div className="flex flex-col items-start gap-1">
                            <StatusChip tone={getGradebookStatusTone(row)}>{getGradebookStatusLabel(row)}</StatusChip>
                            <small className="text-muted">{getGradebookStatusDescription(row)}</small>
                          </div>
                        </td>
                        <td className="p-3">{row.submittedAt ? new Date(row.submittedAt).toLocaleString("vi-VN") : "-"}</td>
                        <td className="p-3">
                          <div className="flex justify-end gap-2">
                            <Button
                              variant="ghost"
                              disabled={!row.canOpenDetail || !row.resultId}
                              onClick={() => {
                                if (!row.resultId || !row.canOpenDetail) return;
                                setSelectedResultId(row.resultId);
                                setExpandedQuestionId(null);
                              }}
                            >
                              <Eye size={15} /> Chi tiết
                            </Button>
                            <Button variant="secondary" onClick={() => setSelectedLogStudentId(row.studentId)}>
                              <FileClock size={15} /> Xem log
                            </Button>
                            <Button variant="secondary" disabled={!row.canAdjust} onClick={() => handleAdjust(row)}><SlidersHorizontal size={15} /> Sửa</Button>
                            <Button
                              loading={publishOne.isPending}
                              disabled={!row.canPublish || !row.resultId}
                              onClick={() => {
                                if (row.resultId && row.canPublish) publishOne.mutate(row.resultId);
                              }}
                            >
                              <Send size={15} /> Duyệt
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>

            <ResultDetailDialog
              open={Boolean(selectedResultId)}
              detail={detailQuery.data}
              loading={detailQuery.isLoading}
              error={detailQuery.error ? getApiErrorMessage(detailQuery.error) : null}
              expandedQuestionId={expandedQuestionId}
              onToggleQuestion={(questionId) => setExpandedQuestionId(expandedQuestionId === questionId ? null : questionId)}
              onOpenChange={(open) => {
                if (!open) {
                  setSelectedResultId(null);
                  setExpandedQuestionId(null);
                }
              }}
            />
            <StudentLogDialog
              open={Boolean(selectedLogStudentId)}
              participant={selectedLogParticipant}
              events={selectedLogEvents}
              onOpenChange={(open) => {
                if (!open) setSelectedLogStudentId(null);
              }}
            />
          </>
        )}
      </DataState>
    </div>
  );
}

function StudentLogDialog({
  open,
  participant,
  events,
  onOpenChange,
}: {
  open: boolean;
  participant?: MonitorParticipant;
  events: MonitorEvent[];
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 max-h-[86vh] w-[min(94vw,720px)] -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-xl border border-line bg-surface p-6 shadow-soft">
          <Dialog.Title className="m-0 text-2xl">Log làm bài</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm text-muted">
            {participant
              ? `${participant.studentName} - ${participant.studentCode || participant.studentId}`
              : "Không tìm thấy học sinh trong snapshot giám sát hiện tại."}
          </Dialog.Description>
          <Dialog.Close className="absolute right-4 top-4 rounded-lg p-2" aria-label="Đóng">
            <X className="h-5 w-5" />
          </Dialog.Close>
          {participant && (
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              <DetailLine label="Trạng thái" value={getStatusLabel(participant.status)} />
              <DetailLine label="Số vi phạm" value={String(participant.totalViolationCount)} />
              <DetailLine label="Lần cuối" value={formatRelativeTime(participant.lastEventAt ?? participant.lastHeartbeatAt ?? participant.lastSeenAt)} />
            </div>
          )}
          {!events.length ? (
            <p className="mb-0 mt-5 rounded-lg border border-line bg-canvas p-4 text-sm text-muted">
              Chưa có log giám sát cho học sinh này trong snapshot hiện tại.
            </p>
          ) : (
            <div className="mt-5 divide-y divide-line rounded-lg border border-line">
              {events.map((event) => (
                <article key={event.id} className="p-4">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <strong className="text-sm text-ink">{getEventLabel(event.eventType)}</strong>
                    <StatusChip tone={getSeverityTone(event.severity)}>{event.severity}</StatusChip>
                  </div>
                  <p className="my-1 text-sm text-muted">{formatEventSummary(event)}</p>
                  <small className="text-xs text-muted">
                    {new Date(event.occurredAt).toLocaleString("vi-VN")}
                    {" - "}
                    {formatRelativeTime(event.occurredAt)}
                  </small>
                </article>
              ))}
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function ResultDetailDialog({
  open,
  detail,
  loading,
  error,
  expandedQuestionId,
  onToggleQuestion,
  onOpenChange,
}: {
  open: boolean;
  detail?: TeacherResultDetail;
  loading: boolean;
  error: string | null;
  expandedQuestionId: string | null;
  onToggleQuestion: (questionId: string) => void;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 flex max-h-[90vh] w-[min(96vw,1100px)] -translate-x-1/2 -translate-y-1/2 flex-col rounded-xl border border-line bg-surface p-6 shadow-soft">
          <Dialog.Title className="m-0 text-2xl">Chi tiết bài làm</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm text-muted">
            {detail ? `${detail.summary.studentName} - ${detail.summary.studentCode || detail.summary.studentId}` : "Đang tải chi tiết bài làm."}
          </Dialog.Description>
          <Dialog.Close className="absolute right-4 top-4 rounded-lg p-2" aria-label="Đóng">
            <X className="h-5 w-5" />
          </Dialog.Close>
          <div className="mt-5 min-h-0 flex-1 overflow-y-auto pr-1">
            <DataState loading={loading} error={error}>
              {detail && (
                <div className="space-y-4">
                  <div className="grid gap-3 md:grid-cols-4">
                    <Metric label="Điểm" value={detail.summary.effectiveScore.toFixed(2)} />
                    <Metric label="Phần trăm" value={`${detail.summary.percentage.toFixed(1)}%`} />
                    <Metric label="Xếp hạng" value={`#${detail.summary.rank}`} />
                    <Metric label="Số câu" value={String(detail.summary.totalQuestions)} />
                  </div>
                  <div className="overflow-x-auto">
                    <div className="min-w-[820px] divide-y divide-line rounded-lg border border-line">
                      {detail.answers.map((answer) => (
                        <QuestionResultRow
                          key={answer.questionId}
                          answer={answer}
                          expanded={expandedQuestionId === answer.questionId}
                          onToggle={() => onToggleQuestion(answer.questionId)}
                        />
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </DataState>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl border border-line bg-surface p-4 shadow-soft"><strong className="text-2xl">{value}</strong><small className="block text-muted">{label}</small></div>;
}

function getGradebookStatusLabel(row: GradebookResultRow) {
  if (row.outcome !== "GRADED") return outcomeLabels[row.outcome];
  return reviewLabels[row.reviewStatus as ResultReviewStatus];
}

function getGradebookStatusDescription(row: GradebookResultRow) {
  if (row.outcome === "LOCKED") return "0 điểm";
  if (row.outcome === "NOT_JOIN") return "0 điểm";
  return visibilityLabels[row.visibilityState as ResultVisibilityState];
}

function getGradebookStatusTone(row: GradebookResultRow): "neutral" | "success" | "warning" | "danger" {
  if (row.outcome === "LOCKED") return "danger";
  if (row.outcome === "NOT_JOIN") return "neutral";
  return row.reviewStatus === "RELEASED" ? "success" : "warning";
}

function QuestionResultRow({
  answer,
  expanded,
  onToggle,
}: {
  answer: TeacherResultAnswer;
  expanded: boolean;
  onToggle: () => void;
}) {
  const selectedLabels = answer.options
    .filter((option) => option.selected)
    .map((option) => option.key || option.optionId);

  return (
    <div>
      <button
        type="button"
        className="grid w-full grid-cols-[72px_1fr_120px_120px_36px] items-center gap-3 px-3 py-4 text-left text-sm transition hover:bg-ink/5"
        onClick={onToggle}
      >
        <span className="font-semibold text-ink">Câu {answer.questionOrder}</span>
        <span className="min-w-0">
          <span className="block truncate font-medium text-ink">{answer.question.content || `Câu hỏi ${answer.questionId}`}</span>
          <span className="block text-xs text-muted">{answer.question.difficulty || "Chưa rõ độ khó"}</span>
        </span>
        <StatusChip tone={answerTones[answer.answerState]}>{answerLabels[answer.answerState]}</StatusChip>
        <span className="text-sm font-semibold text-primary">{answer.scoreAwarded.toFixed(2)} / {answer.maxScore.toFixed(2)}</span>
        <span className="text-muted">{expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}</span>
      </button>
      {expanded && (
        <div className="space-y-4 bg-ink/[0.02] px-5 pb-5 pt-1">
          <div>
            <small className="block text-muted">Nội dung câu hỏi</small>
            <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-ink">{answer.question.content || "Chưa có nội dung câu hỏi."}</p>
          </div>
          <div className="grid gap-2">
            {answer.options.length > 0 ? answer.options.map((option) => (
              <div
                key={option.optionId}
                className="grid grid-cols-[32px_1fr_auto] items-start gap-3 rounded-lg border border-line bg-surface px-3 py-2 text-sm"
              >
                <strong className="text-ink">{option.key || "-"}</strong>
                <span className="whitespace-pre-wrap text-ink">{option.content || option.optionId}</span>
                <span className="flex flex-wrap justify-end gap-1">
                  {option.selected && <StatusChip tone="warning">Đã chọn</StatusChip>}
                  {option.correct && <StatusChip tone="success">Đáp án đúng</StatusChip>}
                </span>
              </div>
            )) : (
              <p className="m-0 text-sm text-muted">Chưa có dữ liệu đáp án chi tiết.</p>
            )}
          </div>
          <div className="grid gap-3 text-sm md:grid-cols-3">
            <DetailLine label="Đáp án đã chọn" value={selectedLabels.length ? selectedLabels.join(", ") : "Bỏ chọn"} />
            <DetailLine label="Ghi chú chấm" value={answer.gradingNote || answerLabels[answer.answerState]} />
            <DetailLine label="Mã câu hỏi" value={answer.questionId} />
          </div>
        </div>
      )}
    </div>
  );
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return <div><small className="block text-muted">{label}</small><strong className="break-words text-ink">{value}</strong></div>;
}

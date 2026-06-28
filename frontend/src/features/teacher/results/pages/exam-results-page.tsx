import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Link, useLocation, useParams } from "react-router-dom";
import { Download, Eye, Send, SlidersHorizontal } from "lucide-react";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import {
  adjustTeacherResultScore,
  exportTeacherExamResults,
  getTeacherExamResults,
  getTeacherResultDetail,
  publishTeacherExamResults,
  publishTeacherResult,
} from "@/features/teacher/results/api/result-repository";
import type { ResultReviewStatus, ResultVisibilityState, TeacherResultRow } from "@/features/teacher/results/model/result-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

interface ResultLocationState {
  backTo?: string;
  examTitle?: string;
}

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

export function ExamResultsPage() {
  const { examId = "" } = useParams();
  const location = useLocation();
  const state = location.state as ResultLocationState | null;
  const queryClient = useQueryClient();
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);

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

  const handleAdjust = (row: TeacherResultRow) => {
    const scoreValue = window.prompt("Nhập điểm mới", String(row.effectiveScore));
    if (scoreValue == null) return;
    const reason = window.prompt("Lý do điều chỉnh");
    if (!reason) return;
    const adjustedScore = Number(scoreValue);
    if (Number.isNaN(adjustedScore)) return;
    adjustScore.mutate({ resultId: row.resultId, adjustedScore, reason });
  };

  const results = query.data?.students.content ?? [];

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
        empty={query.isSuccess && results.length === 0}
        onRetry={() => void query.refetch()}
      >
        {query.data && (
          <>
            <div className="grid gap-3 md:grid-cols-5">
              <Metric label="Đã chấm" value={`${query.data.gradedCount}/${query.data.participantCount}`} />
              <Metric label="Đã phát hành" value={String(query.data.releasedCount)} />
              <Metric label="Chờ duyệt" value={String(query.data.pendingReviewCount)} />
              <Metric label="Trung bình" value={query.data.average.toFixed(1)} />
              <Metric label="Cao nhất" value={query.data.highest.toFixed(1)} />
            </div>

            <section className="grid gap-5 xl:grid-cols-[360px_1fr]">
              <div className="h-80 rounded-xl border border-line bg-surface p-5 shadow-soft">
                <h2 className="mt-0 text-xl">Phân bố điểm</h2>
                <ResponsiveContainer width="100%" height="85%">
                  <BarChart data={query.data.distribution}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="range" />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="count" fill="#c2652a" />
                  </BarChart>
                </ResponsiveContainer>
              </div>

              <div className="overflow-x-auto rounded-xl border border-line bg-surface shadow-soft">
                <table className="w-full min-w-[920px] border-collapse text-sm">
                  <thead className="bg-ink/5 text-left text-xs uppercase text-muted">
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
                    {results.map((row) => (
                      <tr key={row.resultId} className="border-t border-line">
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
                            <StatusChip tone={row.reviewStatus === "RELEASED" ? "success" : "warning"}>{reviewLabels[row.reviewStatus]}</StatusChip>
                            <small className="text-muted">{visibilityLabels[row.visibilityState]}</small>
                          </div>
                        </td>
                        <td className="p-3">{new Date(row.submittedAt).toLocaleString("vi-VN")}</td>
                        <td className="p-3">
                          <div className="flex justify-end gap-2">
                            <Button variant="ghost" onClick={() => setSelectedResultId(row.resultId)}><Eye size={15} /> Chi tiết</Button>
                            <Button variant="secondary" onClick={() => handleAdjust(row)}><SlidersHorizontal size={15} /> Sửa</Button>
                            <Button
                              loading={publishOne.isPending}
                              disabled={row.reviewStatus === "RELEASED"}
                              onClick={() => publishOne.mutate(row.resultId)}
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

            {selectedResultId && (
              <section className="rounded-xl border border-line bg-surface p-5 shadow-soft">
                <div className="flex items-start justify-between gap-4">
                  <h2 className="m-0 text-xl">Chi tiết bài làm</h2>
                  <Button variant="secondary" onClick={() => setSelectedResultId(null)}>Đóng</Button>
                </div>
                <DataState loading={detailQuery.isLoading} error={detailQuery.error ? getApiErrorMessage(detailQuery.error) : null}>
                  {detailQuery.data && (
                    <div className="mt-4 space-y-3">
                      <div className="grid gap-3 md:grid-cols-4">
                        <Metric label="Điểm" value={detailQuery.data.summary.effectiveScore.toFixed(2)} />
                        <Metric label="Phần trăm" value={`${detailQuery.data.summary.percentage.toFixed(1)}%`} />
                        <Metric label="Xếp hạng" value={`#${detailQuery.data.summary.rank}`} />
                        <Metric label="Số câu" value={String(detailQuery.data.summary.totalQuestions)} />
                      </div>
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[760px] border-collapse text-sm">
                          <thead className="bg-ink/5 text-left text-xs uppercase text-muted">
                            <tr><th className="p-3">Câu</th><th className="p-3">Kết quả</th><th className="p-3">Điểm</th><th className="p-3">Đáp án đã chọn</th></tr>
                          </thead>
                          <tbody>
                            {detailQuery.data.answers.map((answer) => (
                              <tr key={answer.questionId} className="border-t border-line">
                                <td className="p-3">{answer.questionOrder}</td>
                                <td className="p-3"><StatusChip tone={answer.correct ? "success" : "danger"}>{answer.gradingNote || (answer.correct ? "Đúng" : "Sai")}</StatusChip></td>
                                <td className="p-3">{answer.scoreAwarded.toFixed(2)} / {answer.maxScore.toFixed(2)}</td>
                                <td className="p-3 text-xs text-muted">{answer.selectedOptionIds.join(", ") || "Bỏ trống"}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                </DataState>
              </section>
            )}
          </>
        )}
      </DataState>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl border border-line bg-surface p-4 shadow-soft"><strong className="text-2xl">{value}</strong><small className="block text-muted">{label}</small></div>;
}

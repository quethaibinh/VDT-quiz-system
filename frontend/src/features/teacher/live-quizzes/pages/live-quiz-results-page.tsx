import { useMutation, useQuery } from "@tanstack/react-query";
import { Award, Download, RefreshCw, Target, Trophy, Users } from "lucide-react";
import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import {
  LiveQuizLeaderboard,
  type LiveQuizMetric,
  type LiveQuizRankEntry,
} from "@/features/live-quizzes/components/live-quiz-leaderboard";
import {
  exportTeacherLiveQuizResults,
  getTeacherLiveQuizResultDetail,
  getTeacherLiveQuizResults,
  liveQuizResultKeys,
  type TeacherLiveQuizResultAnswer,
  type TeacherLiveQuizResultDetail,
  type TeacherLiveQuizResultRow,
  type TeacherLiveQuizResults,
} from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizResultsPage() {
  const { roomId = "" } = useParams();
  const [selectedResultId, setSelectedResultId] = useState<string | null>(null);
  const query = useQuery({
    queryKey: liveQuizResultKeys.list(roomId),
    queryFn: () => getTeacherLiveQuizResults(roomId, { size: 100 }),
    enabled: Boolean(roomId),
    retry: true,
  });
  const detail = useQuery({
    queryKey: selectedResultId ? liveQuizResultKeys.detail(roomId, selectedResultId) : ["teacher", "live-quiz-results", roomId, "none"],
    queryFn: () => getTeacherLiveQuizResultDetail(roomId, selectedResultId!),
    // Chi load chi tiet khi giao vien chon mot hoc sinh, giu man danh sach nhe hon.
    enabled: Boolean(roomId && selectedResultId),
  });
  const exportMutation = useMutation({
    mutationFn: () => exportTeacherLiveQuizResults(roomId),
    onSuccess: (blob) => {
      // Blob den truc tiep tu result-service export endpoint, tao link tam de tai file ve may.
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `live-quiz-results-${roomId}.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
    },
  });

  const selectedRow = useMemo(
    () => query.data?.rows.content.find((row) => row.resultId === selectedResultId) ?? null,
    [query.data?.rows.content, selectedResultId],
  );

  return (
    <div className="space-y-6">
      <PageHeader
        title={query.data?.quizTitle ?? "Kết quả live quiz"}
        description={query.data ? `${query.data.subjectName} - Mã phòng ${query.data.roomCode}` : "Kết quả chính thức sau khi đóng quiz."}
        action={
          <div className="flex flex-wrap gap-2">
            <Link to={`/teacher/live-quizzes/${roomId}/dashboard`}><Button variant="secondary">Bảng điều khiển</Button></Link>
            <Button variant="secondary" onClick={() => query.refetch()}><RefreshCw size={16} /> Làm mới</Button>
            <Button loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}><Download size={16} /> Xuất Excel</Button>
          </div>
        }
      />

      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={query.data?.rows.content.length === 0} onRetry={() => query.refetch()}>
        {query.data && (
          <>
            <LiveQuizLeaderboard
              leaderboard={mapResultLeaderboard(query.data, selectedResultId, setSelectedResultId)}
              leaderboardBadge="Final"
              leaderboardTitle="Bảng xếp hạng"
              metrics={mapResultMetrics(query.data)}
              podiumTitle="Top học sinh"
              progressRows={mapResultProgress(query.data, selectedResultId, setSelectedResultId)}
              progressTitle="Tiến độ kết quả từng học sinh"
            />
            {selectedResultId && (
              <ResultDetailPanel
                detail={detail.data}
                error={detail.error ? getApiErrorMessage(detail.error) : null}
                loading={detail.isLoading}
                row={selectedRow}
                onClose={() => setSelectedResultId(null)}
              />
            )}
          </>
        )}
      </DataState>
    </div>
  );
}

function mapResultMetrics(results: TeacherLiveQuizResults): LiveQuizMetric[] {
  return [
    { label: "Tổng học sinh", value: String(results.participantCount), icon: <Users size={17} /> },
    { label: "Trung bình", value: score(results.averageScore), icon: <Target size={17} /> },
    { label: "Cao nhất", value: score(results.highestScore), icon: <Trophy size={17} /> },
    { label: "Thấp nhất", value: score(results.lowestScore), icon: <Award size={17} /> },
  ];
}

function mapResultLeaderboard(
  results: TeacherLiveQuizResults,
  selectedResultId: string | null,
  onSelect: (resultId: string) => void,
): LiveQuizRankEntry[] {
  return results.rows.content
    .slice()
    .sort((a, b) => a.finalRank - b.finalRank)
    .map((row) => mapResultRow(row, selectedResultId, onSelect));
}

function mapResultProgress(
  results: TeacherLiveQuizResults,
  selectedResultId: string | null,
  onSelect: (resultId: string) => void,
): LiveQuizRankEntry[] {
  return mapResultLeaderboard(results, selectedResultId, onSelect);
}

function mapResultRow(
  row: TeacherLiveQuizResultRow,
  selectedResultId: string | null,
  onSelect: (resultId: string) => void,
): LiveQuizRankEntry {
  return {
    id: row.resultId,
    rank: row.finalRank,
    name: row.studentName,
    code: row.studentCode ?? row.studentId,
    score: row.score,
    maxScore: row.maxScore,
    answeredCount: row.answeredCount,
    totalQuestions: row.totalQuestions,
    correctCount: row.correctCount,
    wrongCount: row.wrongCount,
    timeoutCount: row.timeoutCount,
    notReachedCount: row.notReachedCount,
    averageResponseMs: row.averageResponseMs,
    selected: selectedResultId === row.resultId,
    actionLabel: "Xem",
    onAction: () => onSelect(row.resultId),
    statusLabel: `${score(row.percentage)}%`,
    statusTone: row.percentage >= 80 ? "success" : row.percentage >= 50 ? "warning" : "danger",
  };
}

function ResultDetailPanel({
  detail,
  error,
  loading,
  onClose,
  row,
}: {
  detail: TeacherLiveQuizResultDetail | undefined;
  error: string | null;
  loading: boolean;
  onClose: () => void;
  row: TeacherLiveQuizResultRow | null;
}) {
  return (
    <section className="rounded-2xl border border-line bg-surface p-5 shadow-soft">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="m-0 text-xl">Chi tiết câu trả lời</h2>
          {row ? (
            <p className="m-0 text-sm text-muted">
              #{row.finalRank} - {row.studentName} - {score(row.score)} / {score(row.maxScore)}
            </p>
          ) : null}
        </div>
        <Button variant="ghost" onClick={onClose}>Đóng</Button>
      </div>
      <DataState loading={loading} error={error}>
        {detail && (
          <div className="grid gap-3 md:grid-cols-2">
            {/* Detail nay danh cho giao vien: co trang thai ANSWERED/TIMEOUT/NOT_REACHED va diem tung cau. */}
            {detail.answers.map((answer) => <AnswerDetailCard key={`${answer.questionId}-${answer.questionPosition}`} answer={answer} />)}
          </div>
        )}
      </DataState>
    </section>
  );
}

function AnswerDetailCard({ answer }: { answer: TeacherLiveQuizResultAnswer }) {
  const tone = answerTone(answer);
  return (
    <article className="rounded-xl border border-line/70 bg-canvas/50 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-primary text-sm font-black text-white">
            {answer.questionPosition}
          </span>
          <div>
            <p className="m-0 text-sm font-black text-ink">Câu {answer.questionPosition}</p>
            <p className="m-0 text-xs text-muted">
              {score(answer.scoreAwarded)} / {score(answer.maxScore)} điểm
            </p>
          </div>
        </div>
        <StatusChip tone={tone}>{answerStatusLabel(answer)}</StatusChip>
      </div>
      <p className="mt-3 text-sm text-ink">{answer.questionSnapshot?.content ?? answer.questionId}</p>
      <div className="mt-3 flex flex-wrap gap-2 text-xs font-semibold text-muted">
        {answer.responseTimeMs ? <span>{score(answer.responseTimeMs / 1000)}s</span> : null}
        {answer.selectedOptionIds.length > 0 ? <span>Đã chọn {answer.selectedOptionIds.length} đáp án</span> : null}
        {answer.correctOptionIds.length > 0 ? <span>Đáp án đúng {answer.correctOptionIds.length}</span> : null}
      </div>
    </article>
  );
}

function answerTone(answer: TeacherLiveQuizResultAnswer): "neutral" | "success" | "warning" | "danger" {
  if (answer.answerStatus === "ANSWERED" && answer.correct) return "success";
  if (answer.answerStatus === "ANSWERED") return "danger";
  if (answer.answerStatus === "NOT_REACHED") return "neutral";
  return "warning";
}

function answerStatusLabel(answer: TeacherLiveQuizResultAnswer) {
  if (answer.answerStatus === "ANSWERED" && answer.correct) return "Đúng";
  if (answer.answerStatus === "ANSWERED") return "Sai";
  if (answer.answerStatus === "TIMEOUT") return "Timeout";
  if (answer.answerStatus === "NOT_REACHED") return "Chưa tới";
  return answer.answerStatus;
}

function score(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

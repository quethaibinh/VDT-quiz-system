import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DoorClosed, RefreshCw, Signal, Trophy, Users, WifiOff } from "lucide-react";
import { useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import {
  LiveQuizLeaderboard,
  type LiveQuizMetric,
  type LiveQuizRankEntry,
} from "@/features/live-quizzes/components/live-quiz-leaderboard";
import { applyLiveQuizRealtimeMessage } from "@/features/live-quizzes/realtime/live-quiz-reducer";
import { useLiveQuizRealtime } from "@/features/live-quizzes/realtime/use-live-quiz-realtime";
import {
  closeLiveQuizRoom,
  getLiveQuizRoom,
  getLiveQuizTeacherSnapshot,
  getTeacherLiveQuizResults,
  liveQuizKeys,
  liveQuizResultKeys,
  type LiveQuizTeacherSnapshot,
} from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizDashboardPage() {
  const { roomId = "" } = useParams();
  const queryClient = useQueryClient();
  const [liveSnapshot, setLiveSnapshot] = useState<LiveQuizTeacherSnapshot | undefined>();

  const room = useQuery({
    queryKey: liveQuizKeys.room(roomId),
    queryFn: () => getLiveQuizRoom(roomId),
    enabled: Boolean(roomId),
    staleTime: 2000,
  });
  const snapshot = useQuery({
    queryKey: liveQuizKeys.snapshot(roomId),
    queryFn: () => getLiveQuizTeacherSnapshot(roomId),
    enabled: Boolean(roomId),
    refetchInterval: (query) => query.state.data?.roomStatus === "STARTED" ? 5000 : false,
  });

  const display = withRoomStatus(liveSnapshot ?? snapshot.data, room.data?.status);
  const roomStartedOrClosed = room.data?.status === "STARTED" || room.data?.status === "CLOSED";

  const finalResults = useQuery({
    queryKey: liveQuizResultKeys.list(roomId),
    queryFn: () => getTeacherLiveQuizResults(roomId, { size: 1 }),
    // Sau khi room CLOSED, result-service co the can vai giay de consume Kafka event.
    // Poll nhe den khi co data de giao vien bam sang man ket qua chinh thuc.
    enabled: Boolean(roomId) && display?.roomStatus === "CLOSED",
    refetchInterval: (query) => query.state.data ? false : 3000,
    retry: true,
  });

  const realtime = useLiveQuizRealtime({
    roomId,
    topics: [`/topic/live-quizzes/${roomId}/teacher-progress`, `/topic/live-quizzes/${roomId}/leaderboard`],
    enabled: Boolean(roomId),
    onReconnect: () => void snapshot.refetch(),
    onMessage: (message) => setLiveSnapshot((current) => applyLiveQuizRealtimeMessage(current ?? snapshot.data, message)),
  });

  const close = useMutation({
    mutationFn: () => closeLiveQuizRoom(roomId),
    onSuccess: async () => {
      // Close da kich hoat finalization o runtime; invalidate snapshot de UI doi sang trang thai CLOSED.
      // Ket qua official se duoc query rieng tu result-service bang finalResults.
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.snapshot(roomId) });
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.room(roomId) });
    },
  });

  if ((display?.roomStatus === "OPEN" || display?.roomStatus === "PREPARING") && !roomStartedOrClosed) {
    return <Navigate to={`/teacher/live-quizzes/${roomId}/lobby`} replace />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={display?.quizTitle ?? "Bảng điều khiển live quiz"}
        description={`${display?.subjectName ?? "Môn học"} - ${display?.questionCount ?? 0} câu - ${connectionText(realtime.connectionStatus)}`}
        action={
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" onClick={() => snapshot.refetch()}>
              <RefreshCw size={16} />
              Làm mới
            </Button>
            <Button variant="danger" loading={close.isPending} disabled={display?.roomStatus === "CLOSED"} onClick={() => close.mutate()}>
              <DoorClosed size={16} />
              Kết thúc và chốt kết quả
            </Button>
          </div>
        }
      />

      <DataState
        loading={snapshot.isLoading}
        error={snapshot.error ? getApiErrorMessage(snapshot.error) : close.error ? getApiErrorMessage(close.error) : null}
        empty={!display}
        onRetry={() => snapshot.refetch()}
      >
        {display && (
          <>
            {display.roomStatus === "CLOSED" && (
              <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-line bg-surface p-4 shadow-soft">
                <div>
                  <p className="m-0 font-bold text-ink">Quiz đã đóng</p>
                  {/* Ket qua chi mo khi Result Service da ingest xong, tranh dua giao vien vao trang rong. */}
                  <p className="m-0 text-sm text-muted">{finalResults.data ? "Kết quả chính thức đã sẵn sàng." : "Đang tổng kết kết quả, vui lòng đợi trong giây lát."}</p>
                </div>
                <Link to={`/teacher/live-quizzes/${roomId}/results`}>
                  <Button disabled={!finalResults.data}>Xem kết quả</Button>
                </Link>
              </section>
            )}
            <LiveQuizLeaderboard
              leaderboard={mapLiveLeaderboard(display)}
              leaderboardTitle="Bảng xếp hạng"
              metrics={mapLiveMetrics(display)}
              podiumTitle="Top học sinh hiện tại"
              progressRows={mapLiveProgress(display)}
              progressTitle="Tiến độ từng học sinh"
            />
          </>
        )}
      </DataState>
    </div>
  );
}

function withRoomStatus(
  snapshot: LiveQuizTeacherSnapshot | undefined,
  roomStatus: LiveQuizTeacherSnapshot["roomStatus"] | undefined,
): LiveQuizTeacherSnapshot | undefined {
  if (!snapshot || !roomStatus) return snapshot;
  if (snapshot.roomStatus === "CLOSED") return snapshot;
  if (snapshot.roomStatus === roomStatus) return snapshot;
  return { ...snapshot, roomStatus };
}

function mapLiveMetrics(snapshot: LiveQuizTeacherSnapshot): LiveQuizMetric[] {
  const total = snapshot.summary.joined + snapshot.summary.inProgress + snapshot.summary.finished + snapshot.summary.disconnected;
  return [
    { label: "Tổng học sinh", value: String(total), icon: <Users size={17} /> },
    { label: "Đang làm", value: String(snapshot.summary.inProgress), icon: <Signal size={17} /> },
    { label: "Đã xong", value: String(snapshot.summary.finished), icon: <Trophy size={17} /> },
    { label: "Mất kết nối", value: String(snapshot.summary.disconnected), icon: <WifiOff size={17} /> },
  ];
}

function mapLiveLeaderboard(snapshot: LiveQuizTeacherSnapshot): LiveQuizRankEntry[] {
  return snapshot.leaderboard.map((entry) => ({
    id: entry.participantId,
    rank: entry.rank,
    name: entry.studentName,
    score: entry.totalScore,
    answeredCount: entry.answeredCount,
    correctCount: entry.correctCount,
    timeoutCount: entry.timeoutCount,
    averageResponseMs: entry.averageResponseMs,
    statusLabel: entry.finished ? "FINISHED" : "LIVE",
    statusTone: entry.finished ? "success" : "warning",
  }));
}

function mapLiveProgress(snapshot: LiveQuizTeacherSnapshot): LiveQuizRankEntry[] {
  return [...snapshot.participants]
    .sort((a, b) => (a.currentRank ?? 9999) - (b.currentRank ?? 9999) || a.studentName.localeCompare(b.studentName))
    .map((participant) => ({
      id: participant.participantId,
      rank: participant.currentRank ?? 0,
      name: participant.studentName,
      code: participant.studentCode ?? participant.studentId.slice(0, 8),
      score: participant.totalScore,
      maxScore: participant.maxScore,
      answeredCount: participant.answeredCount,
      totalQuestions: participant.totalQuestions,
      correctCount: participant.correctCount,
      wrongCount: participant.wrongCount,
      timeoutCount: participant.timeoutCount,
      statusLabel: participant.status,
      statusTone: participant.status === "FINISHED" ? "success" : participant.status === "DISCONNECTED" ? "danger" : "warning",
    }));
}

function connectionText(status: string) {
  if (status === "connected") return "realtime";
  if (status === "reconnecting") return "đang nối lại";
  return "snapshot";
}

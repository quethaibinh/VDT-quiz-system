import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { DoorClosed, RefreshCw, Trophy, Users } from "lucide-react";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import { Navigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { applyLiveQuizRealtimeMessage } from "@/features/live-quizzes/realtime/live-quiz-reducer";
import { useLiveQuizRealtime } from "@/features/live-quizzes/realtime/use-live-quiz-realtime";
import {
  closeLiveQuizRoom,
  getLiveQuizTeacherSnapshot,
  liveQuizKeys,
  type LiveQuizLeaderboardEntry,
  type LiveQuizParticipantSnapshot,
  type LiveQuizTeacherSnapshot,
} from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function LiveQuizDashboardPage() {
  const { roomId = "" } = useParams();
  const queryClient = useQueryClient();
  const [liveSnapshot, setLiveSnapshot] = useState<LiveQuizTeacherSnapshot | undefined>();

  const snapshot = useQuery({
    queryKey: liveQuizKeys.snapshot(roomId),
    queryFn: () => getLiveQuizTeacherSnapshot(roomId),
    enabled: Boolean(roomId),
    refetchInterval: (query) => query.state.data?.roomStatus === "STARTED" ? 5000 : false,
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
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.snapshot(roomId) });
    },
  });

  const display = liveSnapshot ?? snapshot.data;
  if (display?.roomStatus === "OPEN" || display?.roomStatus === "PREPARING") {
    return <Navigate to={`/teacher/live-quizzes/${roomId}/lobby`} replace />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={display?.quizTitle ?? "Bang dieu khien live quiz"}
        description={`${display?.subjectName ?? "Mon hoc"} · ${display?.questionCount ?? 0} cau · ${connectionText(realtime.connectionStatus)}`}
        action={
          <div className="flex gap-2">
            <Button variant="secondary" onClick={() => snapshot.refetch()}>
              <RefreshCw size={16} />
              Lam moi
            </Button>
            <Button variant="danger" loading={close.isPending} disabled={display?.roomStatus === "CLOSED"} onClick={() => close.mutate()}>
              <DoorClosed size={16} />
              Dong phong
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
            <SummaryStrip snapshot={display} />
            <TopThree leaderboard={display.leaderboard} />
            <div className="grid gap-5 xl:grid-cols-[minmax(0,420px)_1fr]">
              <Leaderboard entries={display.leaderboard} />
              <ProgressBoard participants={display.participants} />
            </div>
          </>
        )}
      </DataState>
    </div>
  );
}

function SummaryStrip({ snapshot }: { snapshot: LiveQuizTeacherSnapshot }) {
  const total = snapshot.summary.joined + snapshot.summary.inProgress + snapshot.summary.finished + snapshot.summary.disconnected;
  return (
    <section className="grid gap-3 sm:grid-cols-4">
      <Metric label="Tong" value={total} icon={<Users size={18} />} />
      <Metric label="Dang lam" value={snapshot.summary.inProgress} />
      <Metric label="Da xong" value={snapshot.summary.finished} />
      <Metric label="Mat ket noi" value={snapshot.summary.disconnected} />
    </section>
  );
}

function TopThree({ leaderboard }: { leaderboard: LiveQuizLeaderboardEntry[] }) {
  const top = leaderboard.slice(0, 3);
  return (
    <section className="rounded-xl border border-line bg-surface p-5 shadow-soft">
      <div className="flex items-center gap-2">
        <Trophy size={20} className="text-primary" />
        <h2 className="m-0 text-xl">Top 3 hien tai</h2>
      </div>
      <div className="mt-5 grid gap-3 md:grid-cols-3">
        {[0, 1, 2].map((index) => {
          const entry = top[index];
          return (
            <div key={index} className="min-h-32 rounded-lg border border-line bg-canvas/50 p-4 text-center">
              {entry ? (
                <>
                  <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-primary/10 text-lg font-black text-primary">
                    {entry.rank}
                  </div>
                  <p className="m-0 mt-3 truncate font-black text-ink">{entry.studentName}</p>
                  <p className="m-0 mt-1 text-2xl font-black text-primary">{formatScore(entry.totalScore)}</p>
                </>
              ) : (
                <div className="grid min-h-24 place-items-center text-sm text-muted">Dang cho diem</div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Leaderboard({ entries }: { entries: LiveQuizLeaderboardEntry[] }) {
  return (
    <section className="rounded-xl border border-line bg-surface p-5 shadow-soft">
      <h2 className="m-0 text-xl">Bang xep hang</h2>
      <div className="mt-4 space-y-2">
        {entries.length === 0 ? <p className="text-sm text-muted">Chua co du lieu diem.</p> : entries.map((entry) => (
          <div key={entry.participantId} className="grid grid-cols-[44px_minmax(0,1fr)_80px] items-center gap-3 rounded-lg border border-line bg-canvas/40 p-3">
            <div className="font-black text-primary">#{entry.rank}</div>
            <div className="min-w-0">
              <p className="m-0 truncate font-bold text-ink">{entry.studentName}</p>
              <p className="m-0 text-xs text-muted">{entry.answeredCount} cau · {entry.correctCount} dung</p>
            </div>
            <div className="text-right font-black text-ink">{formatScore(entry.totalScore)}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

function ProgressBoard({ participants }: { participants: LiveQuizParticipantSnapshot[] }) {
  const sorted = useMemo(() => [...participants].sort((a, b) => (a.currentRank ?? 9999) - (b.currentRank ?? 9999)), [participants]);
  return (
    <section className="rounded-xl border border-line bg-surface p-5 shadow-soft">
      <h2 className="m-0 text-xl">Tien do tung hoc sinh</h2>
      <div className="mt-4 space-y-3">
        {sorted.length === 0 ? <p className="text-sm text-muted">Chua co hoc sinh nao.</p> : sorted.map((participant) => {
          const percent = participant.totalQuestions > 0 ? Math.round((participant.answeredCount / participant.totalQuestions) * 100) : 0;
          return (
            <div key={participant.participantId} className="rounded-lg border border-line bg-canvas/40 p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="m-0 truncate font-bold text-ink">{participant.studentName}</p>
                  <p className="m-0 text-xs text-muted">{participant.answeredCount}/{participant.totalQuestions} cau · {participant.correctCount} dung · {participant.timeoutCount} timeout</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusChip tone={participant.status === "FINISHED" ? "success" : participant.status === "DISCONNECTED" ? "danger" : "warning"}>{participant.status}</StatusChip>
                  <span className="font-black text-ink">{formatScore(participant.totalScore)}</span>
                </div>
              </div>
              <div className="mt-3 h-2 overflow-hidden rounded-full bg-line">
                <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${Math.min(100, Math.max(0, percent))}%` }} />
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Metric({ label, value, icon }: { label: string; value: number; icon?: ReactNode }) {
  return (
    <div className="rounded-xl border border-line bg-surface p-4 shadow-soft">
      <div className="flex items-center justify-between text-muted">
        <p className="m-0 text-xs font-semibold">{label}</p>
        {icon}
      </div>
      <p className="m-0 mt-1 text-3xl font-black text-ink">{value}</p>
    </div>
  );
}

function formatScore(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

function connectionText(status: string) {
  if (status === "connected") return "realtime";
  if (status === "reconnecting") return "dang noi lai";
  return "snapshot";
}

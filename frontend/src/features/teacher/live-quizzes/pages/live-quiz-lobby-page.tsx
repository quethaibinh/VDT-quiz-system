import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Clipboard, DoorClosed, DoorOpen, Play, RefreshCw, Users, Wifi } from "lucide-react";
import { useMemo, useState } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { applyLiveQuizRealtimeMessage } from "@/features/live-quizzes/realtime/live-quiz-reducer";
import { useLiveQuizRealtime } from "@/features/live-quizzes/realtime/use-live-quiz-realtime";
import {
  closeLiveQuizRoom,
  getLiveQuizRoom,
  getLiveQuizTeacherSnapshot,
  liveQuizKeys,
  openLiveQuizRoom,
  startLiveQuizRoom,
  type LiveQuizParticipantSnapshot,
  type LiveQuizTeacherSnapshot,
} from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

const statusTone = {
  PREPARING: "warning",
  OPEN: "success",
  STARTED: "success",
  CLOSED: "neutral",
} as const;

const statusText = {
  PREPARING: "Dang chuan bi",
  OPEN: "Dang cho hoc sinh",
  STARTED: "Dang dien ra",
  CLOSED: "Da dong",
} as const;

export function LiveQuizLobbyPage() {
  const { roomId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);
  const [liveSnapshot, setLiveSnapshot] = useState<LiveQuizTeacherSnapshot | undefined>();

  const room = useQuery({
    queryKey: liveQuizKeys.room(roomId),
    queryFn: () => getLiveQuizRoom(roomId),
    enabled: Boolean(roomId),
  });
  const snapshot = useQuery({
    queryKey: liveQuizKeys.snapshot(roomId),
    queryFn: () => getLiveQuizTeacherSnapshot(roomId),
    enabled: Boolean(roomId),
    refetchInterval: (query) => query.state.data?.roomStatus === "OPEN" ? 5000 : false,
  });

  const realtime = useLiveQuizRealtime({
    roomId,
    topics: [`/topic/live-quizzes/${roomId}/lobby`],
    enabled: Boolean(roomId),
    onReconnect: () => {
      void snapshot.refetch();
      void room.refetch();
    },
    onMessage: (message) => {
      setLiveSnapshot((current) => applyLiveQuizRealtimeMessage(current ?? snapshot.data, message));
      if (message.roomStatus === "STARTED") navigate(`/teacher/live-quizzes/${roomId}/dashboard`, { replace: true });
    },
  });

  const open = useMutation({
    mutationFn: () => openLiveQuizRoom(roomId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: liveQuizKeys.room(roomId) }),
        queryClient.invalidateQueries({ queryKey: liveQuizKeys.snapshot(roomId) }),
      ]);
    },
  });
  const start = useMutation({
    mutationFn: () => startLiveQuizRoom(roomId),
    onSuccess: () => navigate(`/teacher/live-quizzes/${roomId}/dashboard`, { replace: true }),
  });
  const close = useMutation({
    mutationFn: () => closeLiveQuizRoom(roomId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: liveQuizKeys.room(roomId) }),
        queryClient.invalidateQueries({ queryKey: liveQuizKeys.snapshot(roomId) }),
      ]);
    },
  });

  const current = room.data;
  const display = liveSnapshot ?? snapshot.data;
  const effectiveStatus = display?.roomStatus ?? current?.status ?? "PREPARING";
  const participants = display?.participants ?? [];
  const code = display?.roomCode ?? current?.roomCode ?? "------";

  const actionError = open.error ?? start.error ?? close.error;

  if (effectiveStatus === "STARTED") {
    return <Navigate to={`/teacher/live-quizzes/${roomId}/dashboard`} replace />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={display?.quizTitle ?? current?.quizTitle ?? "Phong quiz"}
        description={`${display?.subjectName ?? current?.subjectName ?? "Mon hoc"} · ${display?.questionCount ?? current?.questionCount ?? 0} cau`}
      />

      <DataState
        loading={room.isLoading || snapshot.isLoading}
        error={room.error || snapshot.error ? getApiErrorMessage(room.error ?? snapshot.error) : null}
        empty={!current && !display}
        onRetry={() => {
          void room.refetch();
          void snapshot.refetch();
        }}
      >
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
          <section className="rounded-xl border border-line bg-surface p-5 shadow-soft md:p-7">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <StatusChip tone={statusTone[effectiveStatus]}>{statusText[effectiveStatus]}</StatusChip>
              <div className="flex items-center gap-2 text-sm font-semibold text-muted">
                <Wifi size={16} />
                {realtime.connectionStatus === "connected" ? "Realtime" : realtime.connectionStatus === "reconnecting" ? "Dang noi lai" : "Dang ket noi"}
              </div>
            </div>

            <div className="mx-auto mt-8 max-w-3xl text-center">
              <p className="m-0 text-xs font-bold uppercase tracking-wide text-muted">Ma phong</p>
              <div className="mt-3 break-all font-mono text-5xl font-black tracking-[0.12em] text-ink sm:text-7xl md:text-8xl">
                {code}
              </div>
              <div className="mt-5 flex flex-wrap justify-center gap-3">
                <Button
                  variant="secondary"
                  onClick={async () => {
                    await navigator.clipboard.writeText(code);
                    setCopied(true);
                    window.setTimeout(() => setCopied(false), 1400);
                  }}
                >
                  {copied ? <Check size={16} /> : <Clipboard size={16} />}
                  {copied ? "Da copy" : "Copy ma"}
                </Button>
                {effectiveStatus === "PREPARING" && (
                  <Button loading={open.isPending} onClick={() => open.mutate()}>
                    <DoorOpen size={16} />
                    Mo phong
                  </Button>
                )}
                {effectiveStatus === "OPEN" && (
                  <Button loading={start.isPending} onClick={() => start.mutate()}>
                    <Play size={16} />
                    Bat dau ca thi
                  </Button>
                )}
                <Button variant="danger" loading={close.isPending} disabled={effectiveStatus === "CLOSED"} onClick={() => close.mutate()}>
                  <DoorClosed size={16} />
                  Dong phong
                </Button>
              </div>
            </div>

            {actionError && (
              <p role="alert" className="mt-5 rounded-lg bg-danger/10 p-3 text-sm text-danger">
                {getApiErrorMessage(actionError)}
              </p>
            )}
          </section>

          <aside className="rounded-xl border border-line bg-surface p-5 shadow-soft">
            <div className="flex items-center justify-between">
              <h2 className="m-0 text-xl">Lop dang vao</h2>
              <Button variant="ghost" onClick={() => snapshot.refetch()}>
                <RefreshCw size={16} />
              </Button>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <Metric label="Da vao" value={participants.length} />
              <Metric label="San sang" value={display?.summary.joined ?? participants.length} />
            </div>
          </aside>
        </div>

        <section className="rounded-xl border border-line bg-surface p-5 shadow-soft">
          <div className="flex items-center gap-2">
            <Users size={18} />
            <h2 className="m-0 text-xl">Hoc sinh trong phong</h2>
          </div>
          {participants.length === 0 ? (
            <p className="mt-5 rounded-lg border border-dashed border-line p-6 text-center text-muted">
              Chua co hoc sinh nao vao phong.
            </p>
          ) : (
            <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {participants.map((participant) => <ParticipantTile key={participant.participantId} participant={participant} />)}
            </div>
          )}
        </section>
      </DataState>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-line bg-canvas/60 p-3">
      <p className="m-0 text-xs font-semibold text-muted">{label}</p>
      <p className="m-0 mt-1 text-2xl font-black text-ink">{value}</p>
    </div>
  );
}

function ParticipantTile({ participant }: { participant: LiveQuizParticipantSnapshot }) {
  const initials = useMemo(() => participant.studentName.split(/\s+/).filter(Boolean).slice(-2).map((part) => part[0]).join("").toUpperCase(), [participant.studentName]);
  return (
    <div className="flex min-h-20 items-center gap-3 rounded-lg border border-line bg-canvas/40 p-3">
      <div className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-primary/10 font-black text-primary">
        {initials || "HS"}
      </div>
      <div className="min-w-0 flex-1">
        <p className="m-0 truncate font-bold text-ink">{participant.studentName}</p>
        <p className="m-0 text-xs text-muted">{participant.studentCode ?? participant.studentId.slice(0, 8)}</p>
      </div>
      <StatusChip tone={participant.status === "JOINED" ? "success" : "neutral"}>{participant.status}</StatusChip>
    </div>
  );
}

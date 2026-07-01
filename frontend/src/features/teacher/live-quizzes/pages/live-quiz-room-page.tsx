import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, Clipboard, DoorClosed, DoorOpen, RefreshCw } from "lucide-react";
import { useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import {
  closeLiveQuizRoom,
  getLiveQuizRoom,
  liveQuizKeys,
  openLiveQuizRoom,
} from "@/features/teacher/live-quizzes";
import type { LiveQuizRoomStatus } from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

const statusLabel: Record<LiveQuizRoomStatus, [string, "neutral" | "success" | "warning" | "danger"]> = {
  PREPARING: ["Dang chuan bi", "warning"],
  OPEN: ["Dang mo", "success"],
  STARTED: ["Dang lam bai", "success"],
  CLOSED: ["Da dong", "neutral"],
};

export function LiveQuizRoomPage() {
  const { roomId = "" } = useParams();
  const location = useLocation();
  const state = (location.state as { subjectId?: string; quizTitle?: string; roomCode?: string } | null) ?? {};
  const [copied, setCopied] = useState(false);
  const queryClient = useQueryClient();
  const room = useQuery({
    queryKey: liveQuizKeys.room(roomId),
    queryFn: () => getLiveQuizRoom(roomId),
    enabled: Boolean(roomId),
  });
  const open = useMutation({
    mutationFn: () => openLiveQuizRoom(roomId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.room(roomId) });
    },
  });
  const close = useMutation({
    mutationFn: () => closeLiveQuizRoom(roomId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: liveQuizKeys.room(roomId) });
    },
  });

  const current = room.data;
  const effectiveCode = current?.roomCode ?? state.roomCode ?? "------";
  const effectiveStatus = current?.status ?? "PREPARING";

  return (
    <div className="space-y-6">
      <PageHeader
        title={state.quizTitle ?? "Phong quiz"}
        description="Hien ma phong cho hoc sinh va mo phong khi lop san sang."
        action={
          state.subjectId && (
            <Link to={`/teacher/subjects/${state.subjectId}/live-quizzes`} className="inline-flex min-h-10 items-center rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold">
              Quay lai danh sach
            </Link>
          )
        }
      />

      <DataState
        loading={room.isLoading}
        error={room.error ? getApiErrorMessage(room.error) : null}
        empty={!current}
        onRetry={() => room.refetch()}
      >
        {current && (
          <div className="grid gap-6 xl:grid-cols-[1fr_360px]">
            <section className="rounded-xl border border-line bg-surface p-6 shadow-soft">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <StatusChip tone={statusLabel[effectiveStatus][1]}>{statusLabel[effectiveStatus][0]}</StatusChip>
                <Button variant="ghost" onClick={() => room.refetch()}>
                  <RefreshCw className="h-4 w-4" />
                  Lam moi
                </Button>
              </div>

              <div className="mt-8 text-center">
                <p className="m-0 text-sm font-semibold uppercase tracking-wide text-muted">Ma phong</p>
                <div className="mt-3 font-mono text-6xl font-black tracking-[0.18em] text-ink md:text-8xl">
                  {effectiveCode}
                </div>
                <Button
                  className="mt-5"
                  variant="secondary"
                  onClick={async () => {
                    await navigator.clipboard.writeText(effectiveCode);
                    setCopied(true);
                    window.setTimeout(() => setCopied(false), 1600);
                  }}
                >
                  {copied ? <Check className="h-4 w-4" /> : <Clipboard className="h-4 w-4" />}
                  {copied ? "Da copy" : "Copy ma"}
                </Button>
              </div>

              <div className="mt-8 grid gap-3 sm:grid-cols-2">
                <Button
                  loading={open.isPending}
                  disabled={effectiveStatus !== "PREPARING"}
                  onClick={() => open.mutate()}
                >
                  <DoorOpen className="h-4 w-4" />
                  Mo phong
                </Button>
                <Button
                  variant="danger"
                  loading={close.isPending}
                  disabled={effectiveStatus === "CLOSED"}
                  onClick={() => close.mutate()}
                >
                  <DoorClosed className="h-4 w-4" />
                  Dong phong
                </Button>
              </div>

              {(open.error || close.error) && (
                <p role="alert" className="mt-4 rounded-lg bg-danger/10 p-3 text-sm text-danger">
                  {getApiErrorMessage(open.error ?? close.error)}
                </p>
              )}
            </section>

            <aside className="rounded-xl border border-line bg-surface p-5 shadow-soft">
              <h2 className="m-0 text-xl">Thong tin phong</h2>
              <dl className="mt-4 space-y-3 text-sm">
                <RoomMeta label="Room ID" value={current.roomId} />
                <RoomMeta label="Quiz ID" value={current.examId} />
                <RoomMeta label="Mo luc" value={formatDate(current.openedAt)} />
                <RoomMeta label="Bat dau luc" value={formatDate(current.startedAt)} />
                <RoomMeta label="Dong luc" value={formatDate(current.closedAt)} />
              </dl>
            </aside>
          </div>
        )}
      </DataState>
    </div>
  );
}

function RoomMeta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="font-semibold text-muted">{label}</dt>
      <dd className="m-0 break-all font-medium text-ink">{value}</dd>
    </div>
  );
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleString("vi-VN") : "Chua co";
}

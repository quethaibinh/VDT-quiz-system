import { useQuery } from "@tanstack/react-query";
import { Clock, Users, Wifi } from "lucide-react";
import type { ReactNode } from "react";
import { useEffect } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusChip } from "@/components/ui/status-chip";
import { useLiveQuizRealtime } from "@/features/live-quizzes/realtime/use-live-quiz-realtime";
import { getStudentLiveQuizState, studentLiveQuizKeys } from "@/features/student/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentLiveQuizLobbyPage() {
  const { roomId = "" } = useParams();
  const navigate = useNavigate();
  const state = useQuery({
    queryKey: studentLiveQuizKeys.state(roomId),
    queryFn: () => getStudentLiveQuizState(roomId),
    enabled: Boolean(roomId),
    refetchInterval: (query) => query.state.data?.roomStatus === "OPEN" ? 3000 : false,
  });

  useLiveQuizRealtime({
    roomId,
    topics: [`/user/queue/live-quizzes/${roomId}`, `/topic/live-quizzes/${roomId}/lobby`],
    enabled: Boolean(roomId),
    onReconnect: () => void state.refetch(),
    onMessage: (message) => {
      if (message.roomStatus === "STARTED") navigate(`/student/live-quizzes/${roomId}/play`, { replace: true });
      else void state.refetch();
    },
  });

  useEffect(() => {
    if (state.data?.roomStatus === "STARTED") navigate(`/student/live-quizzes/${roomId}/play`, { replace: true });
  }, [navigate, roomId, state.data?.roomStatus]);

  if (state.data?.roomStatus === "STARTED") {
    return <Navigate to={`/student/live-quizzes/${roomId}/play`} replace />;
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <PageHeader title={state.data?.quizTitle ?? "Phòng chờ live quiz"} description={state.data?.subjectName ?? "Đang tải thông tin phòng"} />
      <DataState
        loading={state.isLoading}
        error={state.error ? getApiErrorMessage(state.error) : null}
        empty={!state.data}
        onRetry={() => state.refetch()}
      >
        {state.data && (
          <section className="rounded-xl border border-line bg-surface p-6 text-center shadow-soft md:p-8">
            <StatusChip tone={state.data.roomStatus === "OPEN" ? "success" : "warning"}>
              {state.data.roomStatus === "OPEN" ? "Đang chờ bắt đầu" : state.data.roomStatus}
            </StatusChip>
            <div className="mx-auto mt-6 grid h-16 w-16 place-items-center rounded-full bg-primary/10 text-primary">
              <Clock size={30} />
            </div>
            <h2 className="mt-5 text-2xl font-black text-ink">Bạn đã vào phòng</h2>
            <p className="mx-auto max-w-md text-sm leading-6 text-muted">
              Giữ màn hình này mở. Khi giáo viên bắt đầu, bài quiz sẽ tự động hiện ra.
            </p>
            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <Info label="Mã phòng" value={state.data.roomCode} />
              <Info label="Số câu" value={state.data.totalQuestions} />
              <Info label="Học sinh" value={state.data.participantCount} icon={<Users size={16} />} />
            </div>
            <div className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-muted">
              <Wifi size={16} />
              Đang lắng nghe tín hiệu bắt đầu
            </div>
          </section>
        )}
      </DataState>
    </div>
  );
}

function Info({ label, value, icon }: { label: string; value: string | number; icon?: ReactNode }) {
  return (
    <div className="rounded-lg border border-line bg-canvas/50 p-4">
      <p className="m-0 flex items-center justify-center gap-1 text-xs font-semibold text-muted">{icon}{label}</p>
      <p className="m-0 mt-1 font-mono text-2xl font-black text-ink">{value}</p>
    </div>
  );
}

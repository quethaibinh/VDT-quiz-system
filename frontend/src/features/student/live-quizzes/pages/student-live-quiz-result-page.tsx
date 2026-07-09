import { useQuery } from "@tanstack/react-query";
import { CheckCircle, Clock, Medal, RotateCcw } from "lucide-react";
import type { ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { Button } from "@/components/ui/button";
import { getStudentLiveQuizFinalResult, studentLiveQuizResultKeys } from "@/features/student/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentLiveQuizResultPage() {
  const { roomId = "" } = useParams();
  const query = useQuery({
    queryKey: studentLiveQuizResultKeys.result(roomId),
    queryFn: () => getStudentLiveQuizFinalResult(roomId),
    enabled: Boolean(roomId),
    // Trang nay co the duoc vao ngay sau khi teacher close.
    // Neu Kafka consumer chua persist xong thi tiep tuc polling den khi result-service tra summary.
    retry: true,
    refetchInterval: (state) => state.state.data ? false : 3000,
  });

  // Loi LIVE_QUIZ_RESULT_* duoc xem la trang thai dang cho, khong phai loi that voi hoc sinh.
  const waiting = query.error && getApiErrorMessage(query.error).includes("LIVE_QUIZ_RESULT");

  return (
    <main className="min-h-screen bg-canvas p-4 md:p-6">
      <div className="mx-auto max-w-4xl">
        <DataState
          loading={query.isLoading}
          error={!waiting && query.error ? getApiErrorMessage(query.error) : null}
          empty={false}
          onRetry={() => query.refetch()}
        >
          {waiting ? (
            <section className="rounded-xl border border-line bg-surface p-8 text-center shadow-soft">
              <Clock className="mx-auto text-primary" size={42} />
              <h1 className="mt-4 text-3xl font-black text-ink">Đang chốt kết quả</h1>
              <p className="text-muted">Giáo viên đã kết thúc quiz. Hệ thống đang đóng băng điểm và thứ hạng cuối cùng.</p>
              <Button className="mt-4" variant="secondary" onClick={() => query.refetch()}>
                <RotateCcw size={16} />
                Làm mới
              </Button>
            </section>
          ) : query.data ? (
            <section className="rounded-xl border border-line bg-surface p-6 shadow-soft md:p-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="m-0 text-sm font-bold text-primary">{query.data.subjectName}</p>
                  <h1 className="m-0 mt-1 text-3xl font-black text-ink">{query.data.quizTitle}</h1>
                  <p className="m-0 mt-1 text-sm text-muted">Mã phòng {query.data.roomCode}</p>
                </div>
                <div className="inline-flex items-center gap-2 rounded-lg border border-success/30 bg-success/10 px-3 py-2 font-bold text-success">
                  <CheckCircle size={18} />
                  Đã chốt
                </div>
              </div>

              <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <Metric label="Điểm" value={`${score(query.data.score)} / ${score(query.data.maxScore)}`} />
                <Metric label="Thứ hạng" value={`#${query.data.finalRank}/${query.data.participantCount}`} icon={<Medal size={18} />} />
                <Metric label="Đúng" value={String(query.data.correctCount)} />
                <Metric label="Sai" value={String(query.data.wrongCount)} />
                <Metric label="Timeout" value={String(query.data.timeoutCount)} />
                <Metric label="Chưa tới" value={String(query.data.notReachedCount)} />
                <Metric label="Tiến độ" value={`${query.data.answeredCount}/${query.data.totalQuestions}`} />
                <Metric label="Phần trăm" value={`${score(query.data.percentage)}%`} />
              </div>

              <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-4 text-sm text-muted">
                <span>Chốt lúc {query.data.releasedAt ? new Date(query.data.releasedAt).toLocaleString("vi-VN") : "-"}</span>
                <Link to="/student/live-quizzes"><Button variant="secondary">Về màn vào quiz</Button></Link>
              </div>
            </section>
          ) : null}
        </DataState>
      </div>
    </main>
  );
}

function Metric({ label, value, icon }: { label: string; value: string; icon?: ReactNode }) {
  return (
    <div className="rounded-lg border border-line bg-canvas/40 p-4">
      <div className="flex items-center justify-between text-muted">
        <span className="text-xs font-bold">{label}</span>
        {icon}
      </div>
      <p className="m-0 mt-1 text-2xl font-black text-ink">{value}</p>
    </div>
  );
}

function score(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

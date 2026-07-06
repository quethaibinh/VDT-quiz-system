import { BarChart3, ChevronDown, DoorOpen, Pencil, Radio } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import type { LiveQuizStatus, LiveQuizSummary } from "@/features/teacher/live-quizzes";

const statusLabel: Record<LiveQuizStatus, [string, "neutral" | "success" | "warning" | "danger"]> = {
  DRAFT: ["Bản nháp", "neutral"],
  PREPARED: ["Đã chuẩn bị", "warning"],
};

interface LiveQuizListItemProps {
  quiz: LiveQuizSummary;
  expanded?: boolean;
  preparing?: boolean;
  onToggle?: () => void;
  onPrepare?: (quiz: LiveQuizSummary) => void;
}

export function LiveQuizListItem({
  quiz,
  expanded = false,
  preparing,
  onToggle,
  onPrepare,
}: LiveQuizListItemProps) {
  const isDraft = quiz.status === "DRAFT";
  const resultReady = quiz.roomStatus === "CLOSED" && Boolean(quiz.roomId);
  const roomPath = quiz.roomStatus === "STARTED"
    ? `/teacher/live-quizzes/${quiz.roomId}/dashboard`
    : `/teacher/live-quizzes/${quiz.roomId}/lobby`;
  const panelId = `live-quiz-details-${quiz.id}`;
  const triggerId = `live-quiz-trigger-${quiz.id}`;

  return (
    <article className="overflow-hidden rounded-xl border border-line bg-surface shadow-soft">
      <div className="grid gap-4 p-5 lg:grid-cols-[1fr_auto] lg:items-center">
        <button
          type="button"
          id={triggerId}
          className="flex min-w-0 items-center gap-3 rounded-lg text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          aria-expanded={expanded}
          aria-controls={expanded ? panelId : undefined}
          onClick={onToggle}
        >
          <span className="min-w-0 flex-1">
            <span className="block truncate text-2xl font-bold text-ink">{quiz.title}</span>
            <span className="mt-2 block text-sm text-muted">
              {quiz.collectionName} · {quiz.questionCount} câu · {quiz.joinPolicy}
            </span>
          </span>
          <StatusChip tone={statusLabel[quiz.status][1]}>{statusLabel[quiz.status][0]}</StatusChip>
          <ChevronDown
            aria-hidden="true"
            className={`h-5 w-5 shrink-0 text-muted transition-transform ${expanded ? "rotate-180" : ""}`}
          />
        </button>

        <div className="flex flex-wrap gap-2 lg:justify-end">
          {isDraft ? (
            <>
              <Button loading={preparing} disabled={!onPrepare} onClick={() => onPrepare?.(quiz)}>
                <Radio className="h-4 w-4" />
                Prepare
              </Button>
              <Link
                to={`/teacher/subjects/${quiz.subjectId}/live-quizzes/${quiz.id}/edit`}
                className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-line bg-surface px-4 py-2 text-sm font-semibold text-ink transition hover:border-primary"
              >
                <Pencil className="h-4 w-4" />
                Sửa
              </Link>
            </>
          ) : resultReady ? (
            <Link
              to={`/teacher/live-quizzes/${quiz.roomId}/results`}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
            >
              <BarChart3 className="h-4 w-4" />
              Xem kết quả
            </Link>
          ) : quiz.roomId ? (
            <Link
              to={roomPath}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary/90"
            >
              <DoorOpen className="h-4 w-4" />
              Vào phòng
            </Link>
          ) : (
            <Button loading={preparing} disabled={!onPrepare} onClick={() => onPrepare?.(quiz)}>
              <DoorOpen className="h-4 w-4" />
              Tạo lại phòng
            </Button>
          )}
        </div>
      </div>

      {expanded && (
        <div
          id={panelId}
          role="region"
          aria-labelledby={triggerId}
          className="grid gap-4 border-t border-line bg-ink/[0.02] p-5 text-sm md:grid-cols-2 xl:grid-cols-4"
        >
          <Detail label="Mã quiz" value={quiz.code} />
          <Detail label="Bộ câu hỏi" value={quiz.collectionName} />
          <Detail label="Trộn câu hỏi" value={quiz.shuffleQuestions ? "Có" : "Không"} />
          <Detail label="Bảng xếp hạng" value={quiz.showLeaderboard ? "Hiển thị" : "Ẩn"} />
          <Detail label="Đáp án đúng" value={quiz.showCorrectAnswer ? "Hiện sau mỗi câu" : "Không hiện"} />
          <Detail label="Snapshot" value={quiz.snapshotVersion > 0 ? `v${quiz.snapshotVersion}` : "Chưa prepare"} />
          <Detail label="Phòng gần nhất" value={quiz.roomCode ? `${quiz.roomCode} - ${quiz.roomStatus}` : "Chưa có phòng"} />
        </div>
      )}
    </article>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <span>
      <span className="block text-xs font-semibold uppercase text-muted">{label}</span>
      <span className="mt-1 block font-semibold text-ink">{value}</span>
    </span>
  );
}

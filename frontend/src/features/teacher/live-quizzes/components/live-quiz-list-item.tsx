import { ChevronDown, DoorOpen, Pencil, Radio } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import type { LiveQuizStatus, LiveQuizSummary } from "@/features/teacher/live-quizzes";

const statusLabel: Record<LiveQuizStatus, [string, "neutral" | "success" | "warning" | "danger"]> = {
  DRAFT: ["Ban nhap", "neutral"],
  PREPARED: ["Da chuan bi", "warning"],
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
              {quiz.collectionName} · {quiz.questionCount} cau · {quiz.joinPolicy}
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
                Sua
              </Link>
            </>
          ) : (
            <Button loading={preparing} disabled={!onPrepare} onClick={() => onPrepare?.(quiz)}>
              <DoorOpen className="h-4 w-4" />
              Vao phong
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
          <Detail label="Ma quiz" value={quiz.code} />
          <Detail label="Bo cau hoi" value={quiz.collectionName} />
          <Detail label="Tron cau hoi" value={quiz.shuffleQuestions ? "Co" : "Khong"} />
          <Detail label="Bang xep hang" value={quiz.showLeaderboard ? "Hien thi" : "An"} />
          <Detail label="Dap an dung" value={quiz.showCorrectAnswer ? "Hien sau moi cau" : "Khong hien"} />
          <Detail label="Snapshot" value={quiz.snapshotVersion > 0 ? `v${quiz.snapshotVersion}` : "Chua prepare"} />
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

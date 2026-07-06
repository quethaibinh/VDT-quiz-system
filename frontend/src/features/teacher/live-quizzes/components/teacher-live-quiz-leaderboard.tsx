import { Eye, Medal, Trophy, Users } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { cn } from "@/lib/cn";

export interface TeacherQuizMetric {
  label: string;
  value: string;
  hint?: string;
  icon?: ReactNode;
}

export interface TeacherQuizRankEntry {
  id: string;
  rank: number;
  name: string;
  code?: string | null;
  score: number;
  maxScore?: number;
  answeredCount?: number;
  totalQuestions?: number;
  correctCount?: number;
  wrongCount?: number;
  timeoutCount?: number;
  notReachedCount?: number;
  averageResponseMs?: number | null;
  progressPercent?: number;
  statusLabel?: string;
  statusTone?: "neutral" | "success" | "warning" | "danger";
  actionLabel?: string;
  onAction?: () => void;
  selected?: boolean;
}

interface TeacherLiveQuizLeaderboardProps {
  metrics: TeacherQuizMetric[];
  leaderboard: TeacherQuizRankEntry[];
  progressRows: TeacherQuizRankEntry[];
  podiumTitle?: string;
  leaderboardTitle?: string;
  leaderboardBadge?: string;
  progressTitle?: string;
  leaderboardEmptyText?: string;
  progressEmptyText?: string;
}

export function TeacherLiveQuizLeaderboard({
  metrics,
  leaderboard,
  progressRows,
  podiumTitle = "Current Top Explorers",
  leaderboardTitle = "Bảng xếp hạng",
  leaderboardBadge = "Live",
  progressTitle = "Tiến độ từng học sinh",
  leaderboardEmptyText = "Chưa có dữ liệu điểm.",
  progressEmptyText = "Chưa có học sinh nào.",
}: TeacherLiveQuizLeaderboardProps) {
  return (
    <div className="space-y-5">
      <QuizMetricStrip metrics={metrics} />
      <QuizPodium entries={leaderboard} title={podiumTitle} />
      <div className="grid gap-5 xl:grid-cols-[minmax(260px,360px)_1fr]">
        <QuizLeaderboardList
          badge={leaderboardBadge}
          entries={leaderboard}
          emptyText={leaderboardEmptyText}
          title={leaderboardTitle}
        />
        <QuizProgressList
          emptyText={progressEmptyText}
          rows={progressRows}
          title={progressTitle}
        />
      </div>
    </div>
  );
}

function QuizMetricStrip({ metrics }: { metrics: TeacherQuizMetric[] }) {
  return (
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric) => (
        <div
          key={metric.label}
          className="min-h-24 rounded-2xl border border-line/70 bg-surface p-4 shadow-soft"
        >
          <div className="flex items-center justify-between gap-3 text-muted">
            <p className="m-0 text-xs font-bold">{metric.label}</p>
            <span className="text-primary">{metric.icon ?? <Users size={16} />}</span>
          </div>
          <p className="m-0 mt-2 text-4xl font-black leading-none text-ink">{metric.value}</p>
          {metric.hint ? <p className="m-0 mt-1 text-xs text-muted">{metric.hint}</p> : null}
        </div>
      ))}
    </section>
  );
}

function QuizPodium({ entries, title }: { entries: TeacherQuizRankEntry[]; title: string }) {
  const byRank = new Map(entries.map((entry) => [entry.rank, entry]));
  const visualOrder = [2, 1, 3];

  return (
    <section aria-label="Top leaderboard podium" className="overflow-hidden rounded-2xl border border-line/70 bg-surface p-4 shadow-soft sm:p-5">
      <div className="flex items-center gap-2">
        <Trophy size={19} className="text-primary" />
        <h2 className="m-0 text-xl">{title}</h2>
      </div>
      <div className="mt-6 grid grid-cols-[minmax(0,1fr)_minmax(0,1.12fr)_minmax(0,1fr)] items-end gap-2 sm:gap-4">
        {visualOrder.map((rank) => (
          <PodiumSlot key={rank} entry={byRank.get(rank)} rank={rank} />
        ))}
      </div>
    </section>
  );
}

function PodiumSlot({ entry, rank }: { entry?: TeacherQuizRankEntry; rank: number }) {
  const isChampion = rank === 1;
  const heightClass = rank === 1 ? "min-h-44 sm:min-h-48" : rank === 2 ? "min-h-36 sm:min-h-40" : "min-h-32 sm:min-h-36";
  const delayClass = rank === 1 ? "[animation-delay:-.4s]" : rank === 2 ? "[animation-delay:-1.1s]" : "[animation-delay:-.75s]";

  return (
    <div className={cn("quiz-float flex flex-col items-center", delayClass)}>
      <div
        className={cn(
          "relative flex w-full flex-col items-center justify-end rounded-t-2xl border px-2 pb-4 pt-8 text-center transition",
          heightClass,
          isChampion
            ? "border-primary bg-primary text-white shadow-[0_16px_32px_rgba(207,31,31,.2)]"
            : "border-line bg-canvas/70 text-ink",
        )}
      >
        <div
          className={cn(
            "absolute -top-6 grid h-12 w-12 place-items-center rounded-full border-4 border-surface text-sm font-black shadow-soft",
            isChampion ? "bg-white text-primary" : "bg-primary/10 text-primary",
          )}
        >
          {entry ? initials(entry.name) : rank}
        </div>
        {isChampion ? (
          <span className="absolute -top-9 rounded-full bg-accent px-2 py-1 text-[10px] font-black uppercase text-white">
            Top 1
          </span>
        ) : null}
        <div className={cn("text-2xl font-black leading-none", isChampion ? "text-white" : "text-primary")}>
          {rank}
        </div>
        <p className="m-0 mt-2 max-w-full truncate text-sm font-black">{entry?.name ?? "Đang chờ"}</p>
        <p className={cn("m-0 mt-1 text-2xl font-black leading-none", isChampion ? "text-white" : "text-ink")}>
          {entry ? formatScore(entry.score) : "--"}
        </p>
        <p className={cn("m-0 mt-1 text-[10px] font-bold", isChampion ? "text-white/80" : "text-muted")}>
          Points
        </p>
      </div>
    </div>
  );
}

function QuizLeaderboardList({
  badge,
  entries,
  emptyText,
  title,
}: {
  badge: string;
  entries: TeacherQuizRankEntry[];
  emptyText: string;
  title: string;
}) {
  return (
    <section className="rounded-2xl border border-line/70 bg-surface p-4 shadow-soft">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="m-0 text-lg">{title}</h2>
        <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-black uppercase text-primary">{badge}</span>
      </div>
      <div className="space-y-2">
        {entries.length === 0 ? (
          <p className="rounded-xl border border-dashed border-line p-4 text-sm text-muted">{emptyText}</p>
        ) : (
          entries.map((entry) => (
            <div
              key={entry.id}
              className={cn(
                "grid grid-cols-[44px_minmax(0,1fr)_auto] items-center gap-3 rounded-xl border border-line/70 bg-canvas/50 p-3 transition",
                entry.selected ? "border-primary bg-primary/5" : "",
              )}
            >
              <div className="font-black text-primary">#{entry.rank}</div>
              <div className="min-w-0">
                <p className="m-0 truncate text-sm font-black text-ink">{entry.name}</p>
                <p className="m-0 truncate text-[11px] text-muted">{entry.code ?? supportingText(entry)}</p>
              </div>
              <div className="flex items-center gap-2">
                <div className="text-right">
                  <p className="m-0 text-lg font-black leading-none text-ink">{formatScore(entry.score)}</p>
                  {entry.maxScore != null ? (
                    <p className="m-0 text-[10px] text-muted">/{formatScore(entry.maxScore)}</p>
                  ) : null}
                </div>
                {entry.onAction ? (
                  <Button
                    aria-label={`${entry.actionLabel ?? "Xem"} ${entry.name}`}
                    className="h-9 min-h-9 px-2"
                    onClick={entry.onAction}
                    variant="ghost"
                  >
                    <Eye size={15} />
                  </Button>
                ) : null}
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}

function QuizProgressList({
  emptyText,
  rows,
  title,
}: {
  emptyText: string;
  rows: TeacherQuizRankEntry[];
  title: string;
}) {
  return (
    <section className="rounded-2xl border border-line/70 bg-surface p-4 shadow-soft">
      <div className="mb-4 flex items-center gap-2">
        <Medal size={18} className="text-primary" />
        <h2 className="m-0 text-lg">{title}</h2>
      </div>
      <div className="space-y-3">
        {rows.length === 0 ? (
          <p className="rounded-xl border border-dashed border-line p-4 text-sm text-muted">{emptyText}</p>
        ) : (
          rows.map((row) => <ProgressRow key={row.id} row={row} />)
        )}
      </div>
    </section>
  );
}

function ProgressRow({ row }: { row: TeacherQuizRankEntry }) {
  const percent = clamp(row.progressPercent ?? progressPercent(row));
  return (
    <div
      className={cn(
        "rounded-xl border border-line/70 bg-canvas/50 p-3 transition",
        row.selected ? "border-primary bg-primary/5" : "",
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <p className="m-0 truncate text-sm font-black text-ink">{row.name}</p>
            {row.rank > 0 ? <span className="text-xs font-black text-primary">#{row.rank}</span> : null}
          </div>
          <p className="m-0 text-xs text-muted">{supportingText(row)}</p>
        </div>
        <div className="flex items-center gap-2">
          {row.statusLabel ? <StatusChip tone={row.statusTone ?? "neutral"}>{row.statusLabel}</StatusChip> : null}
          <span className="text-base font-black text-ink">{formatScore(row.score)}</span>
          {row.onAction ? (
            <Button className="h-9 min-h-9 px-3" onClick={row.onAction} variant="ghost">
              <Eye size={15} />
              {row.actionLabel ?? "Xem"}
            </Button>
          ) : null}
        </div>
      </div>
      <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-line/70">
        <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${percent}%` }} />
      </div>
      <div className="mt-2 flex flex-wrap gap-2 text-[11px] font-semibold text-muted">
        {row.correctCount != null ? <span>{row.correctCount} đúng</span> : null}
        {row.wrongCount != null ? <span>{row.wrongCount} sai</span> : null}
        {row.timeoutCount != null ? <span>{row.timeoutCount} timeout</span> : null}
        {row.notReachedCount != null ? <span>{row.notReachedCount} chưa tới</span> : null}
      </div>
    </div>
  );
}

function supportingText(entry: TeacherQuizRankEntry) {
  const progress = entry.totalQuestions != null && entry.answeredCount != null
    ? `${entry.answeredCount}/${entry.totalQuestions} câu`
    : null;
  const correct = entry.correctCount != null ? `${entry.correctCount} đúng` : null;
  return [progress, correct].filter(Boolean).join(" - ") || entry.code || "Học sinh";
}

function progressPercent(entry: TeacherQuizRankEntry) {
  if (!entry.totalQuestions || entry.totalQuestions <= 0 || entry.answeredCount == null) return 0;
  return Math.round((entry.answeredCount / entry.totalQuestions) * 100);
}

function clamp(value: number) {
  return Math.min(100, Math.max(0, value));
}

function initials(name: string) {
  const value = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  return value || "HS";
}

function formatQuizScore(value: number) {
  return Number(value ?? 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });
}

function formatScore(value: number) {
  return formatQuizScore(value);
}

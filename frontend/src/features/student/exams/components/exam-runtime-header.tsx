import { GraduationCap } from "lucide-react";
import { SaveIndicator, type SaveState } from "./save-indicator";
import { UnsupportedSubmitButton } from "./unsupported-submit-button";

// Format thoi gian dang HH:MM:SS
function formatDuration(totalSeconds: number) {
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [String(h).padStart(2, "0"), String(m).padStart(2, "0"), String(s).padStart(2, "0")].join(":");
}

interface ExamRuntimeHeaderProps {
  title: string;
  remainingSeconds: number;
  saveState: SaveState;
  lastSavedAt?: string | null;
}

// Header co dinh cua man hinh lam bai thi
export function ExamRuntimeHeader({ title, remainingSeconds, saveState, lastSavedAt }: ExamRuntimeHeaderProps) {
  return (
    <header className="sticky top-0 z-30 flex flex-wrap items-center justify-between gap-3 border-b border-line bg-surface px-4 py-3 shadow-sm md:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <div className="hidden shrink-0 rounded-lg bg-primary/10 p-2 text-primary sm:block">
          <GraduationCap size={20} />
        </div>
        <div className="min-w-0">
          <h1 className="truncate text-sm font-bold text-ink sm:text-base">{title}</h1>
          <p className="m-0 mt-0.5 text-[10px] font-medium uppercase tracking-wider text-muted">Bài thi trắc nghiệm</p>
        </div>
      </div>

      <div className="flex min-w-0 flex-wrap items-center justify-end gap-2 sm:gap-4">
        <SaveIndicator state={saveState} lastSavedAt={lastSavedAt} />
        <div className={`rounded-lg px-3 py-2 font-mono text-sm font-bold tracking-wider transition ${
          remainingSeconds < 60 ? "bg-danger/10 text-danger" : "bg-ink/5 text-ink"
        }`}>
          {formatDuration(remainingSeconds)}
        </div>
        <UnsupportedSubmitButton />
      </div>
    </header>
  );
}

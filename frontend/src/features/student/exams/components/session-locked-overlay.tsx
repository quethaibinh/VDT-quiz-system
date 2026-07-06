import { LockKeyhole } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { StudentAlert } from "../model/proctoring-contracts";

export function SessionLockedOverlay({ alert, onExit }: { alert?: StudentAlert | null; onExit: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink/60 p-4 backdrop-blur-sm">
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="session-locked-title"
        className="w-full max-w-lg rounded-xl border border-danger/30 bg-surface p-6 text-center shadow-soft"
      >
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-danger/10 text-danger">
          <LockKeyhole size={24} />
        </div>
        <h2 id="session-locked-title" className="m-0 text-xl font-bold text-ink">Bài thi đã bị khóa</h2>
        <p className="mx-auto mt-3 max-w-md text-sm leading-relaxed text-muted">
          Hệ thống đã khóa phiên làm bài do vượt quá số lần vi phạm cho phép. Bạn không thể tiếp tục chỉnh sửa bài làm.
        </p>
        {alert?.message && (
          <p className="mt-4 rounded-lg border border-danger/20 bg-danger/5 p-3 text-sm font-semibold text-danger">
            {alert.message}
          </p>
        )}
        <Button className="mt-5" onClick={onExit}>Thoát ra màn ca thi</Button>
      </section>
    </div>
  );
}

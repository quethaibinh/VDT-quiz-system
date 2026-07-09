import { StatusChip } from "@/components/ui/status-chip";
import type { MonitorEvent } from "@/features/teacher/monitoring/model/monitor-contracts";
import {
  formatEventSummary,
  formatRelativeTime,
  getEventLabel,
  getSeverityTone,
} from "@/features/teacher/monitoring/model/monitor-format";

export function MonitorEventStream({ events }: { events: MonitorEvent[] }) {
  return (
    <aside className="rounded-lg border border-line bg-surface p-4 shadow-soft">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h2 className="m-0 text-base font-bold text-ink">Sự kiện gần đây</h2>
        <span className="text-xs font-semibold text-muted">{events.length}</span>
      </div>
      {!events.length ? (
        <p className="m-0 text-sm text-muted">Chưa có sự kiện giám sát.</p>
      ) : (
        <div className="max-h-[min(60vh,520px)] space-y-3 overflow-y-auto pr-1">
          {events.map((event) => (
            <article key={event.id} className="border-b border-line pb-3 last:border-0 last:pb-0">
              <div className="flex items-start justify-between gap-2">
                <strong className="text-sm text-ink">{getEventLabel(event.eventType)}</strong>
                <StatusChip tone={getSeverityTone(event.severity)}>{event.severity}</StatusChip>
              </div>
              <p className="my-1 text-sm text-muted">{formatEventSummary(event)}</p>
              <small className="text-xs text-muted">
                {formatRelativeTime(event.occurredAt)} · {event.studentId}
              </small>
            </article>
          ))}
        </div>
      )}
    </aside>
  );
}

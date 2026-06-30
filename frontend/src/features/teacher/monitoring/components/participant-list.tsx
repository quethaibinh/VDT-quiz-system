import { ShieldAlert } from "lucide-react";
import { StatusChip } from "@/components/ui/status-chip";
import type { MonitorParticipant } from "@/features/teacher/monitoring/model/monitor-contracts";
import {
  formatProgress,
  formatRelativeTime,
  getRiskLabel,
  getRiskTone,
  getStatusLabel,
  getStatusTone,
} from "@/features/teacher/monitoring/model/monitor-format";

export function ParticipantList({ participants }: { participants: MonitorParticipant[] }) {
  if (!participants.length) {
    return (
      <section className="rounded-lg border border-line bg-surface p-6 text-center text-sm text-muted shadow-soft">
        Không có học sinh phù hợp bộ lọc.
      </section>
    );
  }

  return (
    <section className="overflow-hidden rounded-lg border border-line bg-surface shadow-soft">
      <div className="hidden grid-cols-[minmax(0,1fr)_120px_120px_130px_120px] gap-3 border-b border-line px-4 py-3 text-xs font-bold uppercase text-muted md:grid">
        <span>Học sinh</span>
        <span>Trạng thái</span>
        <span>Tiến độ</span>
        <span>Rủi ro</span>
        <span>Lần cuối</span>
      </div>
      <div className="divide-y divide-line">
        {participants.map((participant) => (
          <article
            key={participant.sessionId ?? participant.studentId}
            className="grid gap-3 p-4 md:grid-cols-[minmax(0,1fr)_120px_120px_130px_120px] md:items-center"
          >
            <div className="min-w-0">
              <strong className="block truncate text-sm text-ink">{participant.studentName}</strong>
              <small className="block truncate text-xs text-muted">{participant.studentCode || participant.studentId}</small>
              {participant.locked && (
                <span className="mt-2 inline-flex items-center gap-1 text-xs font-semibold text-danger">
                  <ShieldAlert size={13} /> Bị khóa
                </span>
              )}
            </div>
            <StatusChip tone={getStatusTone(participant.status)}>{getStatusLabel(participant.status)}</StatusChip>
            <span className="text-sm font-semibold text-ink">{formatProgress(participant)}</span>
            <div className="flex flex-wrap items-center gap-2">
              <StatusChip tone={getRiskTone(participant.riskLevel)}>{getRiskLabel(participant.riskLevel)}</StatusChip>
              <span className="text-xs font-semibold text-danger">{participant.totalViolationCount} vi phạm</span>
            </div>
            <small className="text-xs text-muted">{formatRelativeTime(participant.lastEventAt ?? participant.lastHeartbeatAt ?? participant.lastSeenAt)}</small>
          </article>
        ))}
      </div>
    </section>
  );
}

import { AlertTriangle, Clock3, LockKeyhole, Radio, Send, Users } from "lucide-react";
import type { ReactNode } from "react";
import type { MonitorParticipant } from "@/features/teacher/monitoring/model/monitor-contracts";
import { getMonitorMetrics } from "@/features/teacher/monitoring/model/monitor-state";

export function MonitorMetrics({ participants }: { participants: MonitorParticipant[] }) {
  const metrics = getMonitorMetrics(participants);
  return (
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-6">
      <Metric icon={<Users size={18} />} label="Phân công" value={metrics.total} />
      <Metric icon={<Clock3 size={18} />} label="Chưa tham gia" value={metrics.notJoined} />
      <Metric icon={<Radio size={18} />} label="Online" value={metrics.online} />
      <Metric icon={<AlertTriangle size={18} />} label="Cảnh báo" value={metrics.warning} tone="warning" />
      <Metric icon={<Send size={18} />} label="Đã nộp" value={metrics.submitted} tone="success" />
      <Metric icon={<LockKeyhole size={18} />} label="Bị khóa" value={metrics.locked} tone="danger" />
    </section>
  );
}

function Metric({
  icon,
  label,
  value,
  tone = "neutral",
}: {
  icon: ReactNode;
  label: string;
  value: number;
  tone?: "neutral" | "success" | "warning" | "danger";
}) {
  const tones = {
    neutral: "bg-ink/5 text-ink",
    success: "bg-success/10 text-success",
    warning: "bg-warning/10 text-warning",
    danger: "bg-danger/10 text-danger",
  };
  return (
    <div className="flex min-w-0 items-center gap-3 rounded-lg border border-line bg-surface p-4 shadow-soft">
      <span className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${tones[tone]}`}>{icon}</span>
      <span className="min-w-0">
        <strong className="block text-2xl leading-none text-ink">{value}</strong>
        <small className="block truncate text-xs font-semibold text-muted">{label}</small>
      </span>
    </div>
  );
}

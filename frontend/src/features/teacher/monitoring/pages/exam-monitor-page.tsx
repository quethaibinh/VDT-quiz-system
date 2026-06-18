import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusChip } from "@/components/ui/status-chip";
import { getMonitor } from "@/features/teacher/monitoring/api/monitor-repository";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function ExamMonitorPage() {
  const { examId = "" } = useParams();
  const query = useQuery({ queryKey: ["teacher", "monitor", examId], queryFn: () => getMonitor(examId), refetchInterval: 10_000 });
  const online = query.data?.participants.filter((item) => item.status === "ONLINE").length ?? 0;
  const submitted = query.data?.participants.filter((item) => item.status === "SUBMITTED").length ?? 0;
  return <div className="space-y-6"><PageHeader title="Giám sát ca thi" description="Chế độ demo, làm mới mỗi 10 giây." />
    <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data}>
      {query.data && <><div className="grid gap-3 sm:grid-cols-3"><Metric label="Tham gia" value={query.data.participants.length} /><Metric label="Online" value={online} /><Metric label="Đã nộp" value={submitted} /></div><div className="grid gap-5 xl:grid-cols-[1fr_360px]"><section className="overflow-hidden rounded-xl border border-line bg-surface shadow-soft">{query.data.participants.map((item) => <article key={item.id} className="grid gap-2 border-b border-line p-4 last:border-0 md:grid-cols-[1fr_120px_120px_100px] md:items-center"><strong>{item.name}</strong><StatusChip tone={item.status === "ONLINE" ? "success" : item.status === "OFFLINE" ? "danger" : "neutral"}>{item.status}</StatusChip><span className="text-sm">{item.answered}/{item.total} câu</span><span className="text-sm text-danger">{item.violations} vi phạm</span></article>)}</section><aside className="rounded-xl border border-line bg-surface p-5 shadow-soft"><h2 className="mt-0">Sự kiện gần đây</h2>{query.data.events.map((event) => <div key={event.id} className="border-b border-line py-3 text-sm"><strong>{event.studentName}</strong><p className="my-1 text-muted">{event.type}</p><small>{new Date(event.occurredAt).toLocaleTimeString("vi-VN")}</small></div>)}</aside></div></>}
    </DataState></div>;
}
function Metric({ label, value }: { label: string; value: number }) { return <div className="rounded-xl border border-line bg-surface p-5 shadow-soft"><strong className="text-3xl">{value}</strong><small className="block text-muted">{label}</small></div>; }

import { useQuery } from "@tanstack/react-query";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Link, useLocation, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { getResults } from "@/features/teacher/results/api/result-repository";
import { getApiErrorMessage } from "@/lib/http/api-error";

interface ResultLocationState {
  backTo?: string;
  examTitle?: string;
}

export function ExamResultsPage() {
  const { examId = "" } = useParams();
  const location = useLocation();
  const state = location.state as ResultLocationState | null;
  const query = useQuery({ queryKey: ["teacher", "results", examId], queryFn: () => getResults(examId) });

  return <div className="space-y-6">
    <PageHeader
      title={state?.examTitle || "Kết quả ca thi"}
      description="Thống kê mô phỏng trong lúc Result Service chưa có API."
      action={<div className="flex gap-2"><Link to={state?.backTo || "/teacher/results"}><Button variant="secondary">Quay lại</Button></Link><Button variant="secondary">Xuất Excel</Button></div>}
    />
    <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data}>
      {query.data && <><div className="grid gap-3 sm:grid-cols-4"><Metric label="Đã chấm" value={`${query.data.gradedCount}/${query.data.participantCount}`} /><Metric label="Trung bình" value={query.data.average.toFixed(1)} /><Metric label="Cao nhất" value={query.data.highest.toFixed(1)} /><Metric label="Thấp nhất" value={query.data.lowest.toFixed(1)} /></div><section className="grid gap-5 xl:grid-cols-[420px_1fr]"><div className="h-80 rounded-xl border border-line bg-surface p-5 shadow-soft"><h2 className="mt-0">Phân bố điểm</h2><ResponsiveContainer width="100%" height="85%"><BarChart data={query.data.distribution}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="range" /><YAxis /><Tooltip /><Bar dataKey="count" fill="#c2652a" /></BarChart></ResponsiveContainer></div><div className="overflow-hidden rounded-xl border border-line bg-surface shadow-soft">{query.data.students.map((student) => <article key={student.id} className="grid grid-cols-[1fr_auto_auto] gap-4 border-b border-line p-4 last:border-0"><strong>{student.name}</strong><span>{student.correct} đúng / {student.wrong} sai</span><strong className="text-primary">{student.score.toFixed(1)}</strong></article>)}</div></section></>}
    </DataState>
  </div>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl border border-line bg-surface p-5 shadow-soft"><strong className="text-3xl">{value}</strong><small className="block text-muted">{label}</small></div>;
}

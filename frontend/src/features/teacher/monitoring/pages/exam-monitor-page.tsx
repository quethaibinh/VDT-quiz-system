import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { StatusChip } from "@/components/ui/status-chip";
import { MonitorEventStream } from "@/features/teacher/monitoring/components/monitor-event-stream";
import { MonitorFilters } from "@/features/teacher/monitoring/components/monitor-filters";
import { MonitorMetrics } from "@/features/teacher/monitoring/components/monitor-metrics";
import { MonitorMobileTabs, type MonitorMobileTab } from "@/features/teacher/monitoring/components/monitor-mobile-tabs";
import { ParticipantList } from "@/features/teacher/monitoring/components/participant-list";
import { getMonitor } from "@/features/teacher/monitoring/api/monitor-repository";
import { useMonitorRealtime } from "@/features/teacher/monitoring/hooks/use-monitor-realtime";
import { filterMonitorParticipants, type MonitorFilter } from "@/features/teacher/monitoring/model/monitor-state";
import { getApiErrorMessage } from "@/lib/http/api-error";

const connectionLabels = {
  idle: "Polling",
  connecting: "Đang kết nối",
  connected: "Realtime",
  reconnecting: "Đang nối lại",
  disconnected: "Polling",
  error: "Lỗi realtime",
};

export function ExamMonitorPage() {
  const { examId = "" } = useParams();
  const [filter, setFilter] = useState<MonitorFilter>("ALL");
  const [keyword, setKeyword] = useState("");
  const [mobileTab, setMobileTab] = useState<MonitorMobileTab>("PARTICIPANTS");

  const query = useQuery({
    queryKey: ["teacher", "monitor", examId],
    queryFn: () => getMonitor(examId),
    enabled: Boolean(examId),
    // Polling vẫn là đường fallback/reconcile khi WS miss event hoặc mất kết nối.
    // 10s giúp giáo viên thấy tiến độ autosave gần realtime hơn trong lúc debug/vận hành.
    refetchInterval: 10_000,
  });

  const realtime = useMonitorRealtime(examId, query.data);
  const snapshot = realtime.snapshot ?? query.data;
  const participants = useMemo(() => snapshot?.participants ?? [], [snapshot]);
  const filteredParticipants = useMemo(
    () => filterMonitorParticipants(participants, filter, keyword),
    [filter, keyword, participants],
  );

  const connectionTone = realtime.connectionStatus === "connected"
    ? "success"
    : realtime.connectionStatus === "error"
      ? "danger"
      : "warning";

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <PageHeader title="Giám sát ca thi" description="Theo dõi học sinh, tiến độ và sự kiện vi phạm theo thời gian thực." />
        <StatusChip tone={connectionTone}>{connectionLabels[realtime.connectionStatus]}</StatusChip>
      </div>

      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={!snapshot}
        onRetry={() => query.refetch()}
      >
        {snapshot && (
          <>
            <MonitorMetrics participants={participants} />
            <MonitorFilters
              filter={filter}
              keyword={keyword}
              onFilterChange={setFilter}
              onKeywordChange={setKeyword}
            />
            <MonitorMobileTabs value={mobileTab} onChange={setMobileTab} />

            <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_360px]">
              <div className={mobileTab === "EVENTS" ? "hidden md:block" : "block"}>
                <ParticipantList participants={filteredParticipants} />
              </div>
              <div className={mobileTab === "PARTICIPANTS" ? "hidden md:block" : "block"}>
                <MonitorEventStream events={snapshot.events} />
              </div>
            </div>
          </>
        )}
      </DataState>
    </div>
  );
}

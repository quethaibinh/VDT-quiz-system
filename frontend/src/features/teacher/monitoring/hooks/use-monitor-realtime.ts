import { useEffect, useMemo, useRef, useState } from "react";
import {
  createMonitoringStompClient,
  getStoredAuthToken,
  safeParseMessage,
  type MonitoringConnectionStatus,
} from "@/features/monitoring/realtime/monitoring-stomp-client";
import type { MonitorRealtimeMessage, MonitorSnapshot } from "@/features/teacher/monitoring/model/monitor-contracts";
import { applyMonitorRealtimeMessage } from "@/features/teacher/monitoring/model/monitor-state";

export function useMonitorRealtime(examId: string, snapshot?: MonitorSnapshot) {
  const [mergedSnapshot, setMergedSnapshot] = useState<MonitorSnapshot | undefined>();
  const [connectionStatus, setConnectionStatus] = useState<MonitoringConnectionStatus>("idle");
  const snapshotRef = useRef<MonitorSnapshot | undefined>(snapshot);
  const token = getStoredAuthToken();

  useEffect(() => {
    snapshotRef.current = snapshot;
  }, [snapshot]);

  useEffect(() => {
    if (!examId || !token) {
      return;
    }

    const client = createMonitoringStompClient({ token, onStatusChange: setConnectionStatus });
    client.onConnect = () => {
      setConnectionStatus("connected");
      client.subscribe(`/topic/exams/${examId}/monitor`, (message) => {
        const parsed = safeParseMessage<MonitorRealtimeMessage>(message);
        if (!parsed) return;
        setMergedSnapshot((current) => applyMonitorRealtimeMessage(current ?? snapshotRef.current ?? emptySnapshot(examId), parsed));
      });
    };
    client.activate();

    return () => {
      void client.deactivate();
    };
  }, [examId, token]);

  const effectiveStatus = examId && token ? connectionStatus : "idle";
  const displaySnapshot = reconcileSnapshot(mergedSnapshot, snapshot);
  const realtimeActive = effectiveStatus === "connected";
  return useMemo(() => ({
    snapshot: displaySnapshot,
    connectionStatus: effectiveStatus,
    realtimeActive,
  }), [displaySnapshot, effectiveStatus, realtimeActive]);
}

function reconcileSnapshot(current?: MonitorSnapshot, next?: MonitorSnapshot) {
  if (!next) return current;
  if (!current || current.examId !== next.examId) return next;
  const eventIds = new Set(next.events.map((event) => event.id));
  return {
    ...next,
    events: [
      ...next.events,
      ...current.events.filter((event) => !eventIds.has(event.id)),
    ].slice(0, 100),
  };
}

function emptySnapshot(examId: string): MonitorSnapshot {
  return {
    examId,
    serverTime: new Date().toISOString(),
    participants: [],
    events: [],
  };
}

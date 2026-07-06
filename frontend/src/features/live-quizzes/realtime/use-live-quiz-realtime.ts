import { useEffect, useMemo, useRef, useState } from "react";
import {
  createMonitoringStompClient,
  getStoredAuthToken,
  safeParseMessage,
  type MonitoringConnectionStatus,
} from "@/features/monitoring/realtime/monitoring-stomp-client";
import type { LiveQuizRealtimeMessage } from "@/features/teacher/live-quizzes";

interface UseLiveQuizRealtimeOptions {
  roomId: string;
  topics: string[];
  enabled?: boolean;
  onMessage: (message: LiveQuizRealtimeMessage) => void;
  onReconnect?: () => void;
}

export function useLiveQuizRealtime({
  roomId,
  topics,
  enabled = true,
  onMessage,
  onReconnect,
}: UseLiveQuizRealtimeOptions) {
  const [connectionStatus, setConnectionStatus] = useState<MonitoringConnectionStatus>("idle");
  const onMessageRef = useRef(onMessage);
  const onReconnectRef = useRef(onReconnect);
  const token = getStoredAuthToken();
  const topicKey = topics.join("|");

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    onReconnectRef.current = onReconnect;
  }, [onReconnect]);

  useEffect(() => {
    if (!roomId || !token || !enabled || topics.length === 0) return;
    const client = createMonitoringStompClient({
      token,
      onStatusChange: (status) => {
        setConnectionStatus(status);
        if (status === "connected") onReconnectRef.current?.();
      },
    });

    client.onConnect = () => {
      setConnectionStatus("connected");
      topics.forEach((topic) => {
        client.subscribe(topic, (message) => {
          const parsed = safeParseMessage<LiveQuizRealtimeMessage>(message);
          if (parsed && parsed.roomId === roomId) onMessageRef.current(parsed);
        });
      });
    };
    client.activate();
    return () => {
      void client.deactivate();
    };
    // topics are intentionally represented by topicKey to avoid resubscribing on array identity changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, roomId, token, topicKey]);

  return useMemo(() => ({
    connectionStatus: roomId && token && enabled ? connectionStatus : "idle",
    realtimeActive: connectionStatus === "connected",
  }), [connectionStatus, enabled, roomId, token]);
}

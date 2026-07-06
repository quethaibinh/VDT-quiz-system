import { useCallback, useEffect, useRef, useState } from "react";
import {
  createMonitoringStompClient,
  getStoredAuthToken,
  safeParseMessage,
  type MonitoringConnectionStatus,
} from "@/features/monitoring/realtime/monitoring-stomp-client";
import type { ProctoringEventRequest, StudentAlert, StudentProctoringEventType } from "../model/proctoring-contracts";

const noisyEventCooldownMs = 1500;

export function useStudentProctoring({
  examId,
  sessionId,
  enabled,
}: {
  examId: string;
  sessionId?: string;
  enabled: boolean;
}) {
  const [locked, setLocked] = useState(false);
  const [lastAlert, setLastAlert] = useState<StudentAlert | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<MonitoringConnectionStatus>("idle");
  const clientRef = useRef<ReturnType<typeof createMonitoringStompClient> | null>(null);
  const lastSentAtRef = useRef<Record<string, number>>({});
  const token = getStoredAuthToken();

  const sendEvent = useCallback((eventType: StudentProctoringEventType) => {
    if (!enabled || !examId || !sessionId) return;
    const now = Date.now();
    if (isNoisy(eventType) && now - (lastSentAtRef.current[eventType] ?? 0) < noisyEventCooldownMs) {
      return;
    }
    lastSentAtRef.current[eventType] = now;
    const payload: ProctoringEventRequest = {
      clientEventId: createClientEventId(),
      eventType,
      occurredAt: new Date().toISOString(),
      metadata: JSON.stringify(createMetadata()),
    };
    const client = clientRef.current;
    if (!client?.connected) return;
    client.publish({
      destination: `/app/exams/${examId}/sessions/${sessionId}/events`,
      body: JSON.stringify(payload),
    });
  }, [enabled, examId, sessionId]);

  useEffect(() => {
    if (!enabled || !examId || !sessionId) {
      return;
    }
    if (!token) {
      return;
    }

    const client = createMonitoringStompClient({
      token,
      // Backend can chi hai native header nay luc CONNECT de map stomp session
      // ve runtime session, tu do publish ONLINE/OFFLINE cho giao vien theo realtime.
      connectHeaders: {
        "exam-id": examId,
        "session-id": sessionId,
      },
      onStatusChange: setConnectionStatus,
    });
    clientRef.current = client;
    client.onConnect = () => {
      setConnectionStatus("connected");
      client.subscribe(`/user/queue/exams/${examId}/alerts`, (message) => {
        const alert = safeParseMessage<StudentAlert>(message);
        if (!alert) return;
        setLastAlert(alert);
        if (alert.type === "LOCKED") {
          setLocked(true);
        }
      });
    };
    client.activate();
    return () => {
      clientRef.current = null;
      void client.deactivate();
    };
  }, [enabled, examId, sessionId, token]);

  useEffect(() => {
    if (!enabled) return;

    const onVisibilityChange = () => {
      sendEvent(document.hidden ? "TAB_HIDDEN" : "RETURNED");
    };
    const onBlur = () => sendEvent("WINDOW_BLUR");
    const onFullscreenChange = () => {
      if (!document.fullscreenElement) sendEvent("FULLSCREEN_EXIT");
    };
    const blockAndReport = (event: Event, eventType: StudentProctoringEventType) => {
      event.preventDefault();
      sendEvent(eventType);
    };
    const onCopy = (event: ClipboardEvent) => blockAndReport(event, "COPY_ATTEMPT");
    const onPaste = (event: ClipboardEvent) => blockAndReport(event, "PASTE_ATTEMPT");
    const onContextMenu = (event: MouseEvent) => blockAndReport(event, "CONTEXT_MENU_OPENED");

    document.addEventListener("visibilitychange", onVisibilityChange);
    window.addEventListener("blur", onBlur);
    document.addEventListener("fullscreenchange", onFullscreenChange);
    document.addEventListener("copy", onCopy);
    document.addEventListener("paste", onPaste);
    document.addEventListener("contextmenu", onContextMenu);

    return () => {
      document.removeEventListener("visibilitychange", onVisibilityChange);
      window.removeEventListener("blur", onBlur);
      document.removeEventListener("fullscreenchange", onFullscreenChange);
      document.removeEventListener("copy", onCopy);
      document.removeEventListener("paste", onPaste);
      document.removeEventListener("contextmenu", onContextMenu);
    };
  }, [enabled, sendEvent]);

  return {
    locked,
    setLocked,
    lastAlert,
    connectionStatus: enabled && examId && sessionId && token ? connectionStatus : "idle",
    sendEvent,
  };
}

function createClientEventId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function createMetadata() {
  return {
    fullscreen: Boolean(document.fullscreenElement),
    screenResolution: `${window.screen.width}x${window.screen.height}`,
    visibilityState: document.visibilityState,
    userAgent: window.navigator.userAgent,
  };
}

function isNoisy(eventType: StudentProctoringEventType) {
  return eventType === "WINDOW_BLUR" || eventType === "FULLSCREEN_EXIT";
}

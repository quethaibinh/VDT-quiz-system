import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { env } from "@/lib/env";
import { buildMonitoringWebSocketUrl } from "./monitoring-ws-url";

export type MonitoringConnectionStatus = "idle" | "connecting" | "connected" | "reconnecting" | "disconnected" | "error";

export function getStoredAuthToken() {
  return window.sessionStorage.getItem("sahara.quiz.session");
}

export function createMonitoringStompClient({
  token,
  connectHeaders,
  onStatusChange,
}: {
  token: string;
  connectHeaders?: Record<string, string>;
  onStatusChange?: (status: MonitoringConnectionStatus) => void;
}) {
  const client = new Client({
    brokerURL: buildMonitoringWebSocketUrl(env.apiBaseUrl, token),
    connectHeaders,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => undefined,
    beforeConnect: () => onStatusChange?.("connecting"),
    onConnect: () => onStatusChange?.("connected"),
    onDisconnect: () => onStatusChange?.("disconnected"),
    onStompError: () => onStatusChange?.("error"),
    onWebSocketClose: () => onStatusChange?.(client.active ? "reconnecting" : "disconnected"),
    onWebSocketError: () => onStatusChange?.("error"),
  });
  return client;
}

export function safeParseMessage<T>(message: IMessage): T | null {
  try {
    return JSON.parse(message.body) as T;
  } catch {
    return null;
  }
}

export type { IMessage, StompSubscription };

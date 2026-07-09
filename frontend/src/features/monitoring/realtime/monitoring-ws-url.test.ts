import { describe, expect, it } from "vitest";
import { buildMonitoringWebSocketUrl } from "./monitoring-ws-url";

describe("buildMonitoringWebSocketUrl", () => {
  it("converts http API base to ws monitor endpoint", () => {
    expect(buildMonitoringWebSocketUrl("http://localhost:8080", "token value")).toBe(
      "ws://localhost:8080/v1/api/examruntime-service/ws?access_token=token+value",
    );
  });

  it("converts https API base to wss", () => {
    expect(buildMonitoringWebSocketUrl("https://quiz.example.com/api", "abc")).toBe(
      "wss://quiz.example.com/api/v1/api/examruntime-service/ws?access_token=abc",
    );
  });

  it("supports relative API bases", () => {
    expect(buildMonitoringWebSocketUrl("/gateway", "abc", "https://quiz.example.com")).toBe(
      "wss://quiz.example.com/gateway/v1/api/examruntime-service/ws?access_token=abc",
    );
  });
});

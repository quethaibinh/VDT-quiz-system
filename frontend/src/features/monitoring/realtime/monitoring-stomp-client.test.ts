import { describe, expect, it } from "vitest";
import { createMonitoringStompClient } from "./monitoring-stomp-client";

describe("createMonitoringStompClient", () => {
  it("keeps native connect headers for student session tracking", () => {
    const client = createMonitoringStompClient({
      token: "jwt-token",
      connectHeaders: {
        "exam-id": "exam-1",
        "session-id": "session-1",
      },
    });

    expect(client.connectHeaders).toMatchObject({
      "exam-id": "exam-1",
      "session-id": "session-1",
    });
  });
});

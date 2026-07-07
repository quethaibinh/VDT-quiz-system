import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { StudentLiveQuizPlayPage } from "@/features/student/live-quizzes/pages/student-live-quiz-play-page";
import type { LiveQuizRealtimeMessage } from "@/features/teacher/live-quizzes";

const realtime = vi.hoisted(() => ({
  options: null as null | { onMessage: (message: LiveQuizRealtimeMessage) => void },
}));

vi.mock("@/features/live-quizzes/realtime/use-live-quiz-realtime", () => ({
  useLiveQuizRealtime: (options: { onMessage: (message: LiveQuizRealtimeMessage) => void }) => {
    realtime.options = options;
    return { connectionStatus: "connected", realtimeActive: true };
  },
}));

const envelope = <T,>(data: T) => ({
  timestamp: "2026-07-07T08:00:00Z",
  status: 200,
  message: "Success",
  data,
});

const leaderboard = [{
  rank: 1,
  participantId: "participant-1",
  studentId: "student-1",
  studentName: "Student One",
  totalScore: 8.5,
  answeredCount: 10,
  correctCount: 8,
  timeoutCount: 0,
  averageResponseMs: 1200,
  finished: true,
}];

let showLeaderboard = true;
let roomStatus: "STARTED" | "CLOSED" = "STARTED";

const server = setupServer(
  http.get("*/v1/api/examruntime-service/student/live-quizzes/room-1/state", () =>
    HttpResponse.json(envelope({
      roomId: "room-1",
      examId: "exam-1",
      participantId: "participant-1",
      roomCode: "QZ0001",
      quizTitle: "Live Quiz",
      subjectName: "Math",
      roomStatus,
      participantStatus: "FINISHED",
      answeredCount: 10,
      totalQuestions: 10,
      totalScore: 8.5,
      maxScore: 10,
      currentRank: 1,
      participantCount: 2,
      showLeaderboard,
      leaderboard: showLeaderboard ? leaderboard : [],
      serverTime: "2026-07-07T08:00:00Z",
      currentQuestionEndsAt: null,
    }))),
  http.get("*/v1/api/result-service/student/live-quizzes/room-1/result", () =>
    HttpResponse.json(envelope({
      roomId: "room-1",
      examId: "exam-1",
      roomCode: "QZ0001",
      quizTitle: "Live Quiz",
      subjectId: null,
      subjectName: "Math",
      finalRank: 1,
      participantCount: 2,
      score: 8.5,
      maxScore: 10,
      percentage: 85,
      answeredCount: 10,
      totalQuestions: 10,
      correctCount: 8,
      wrongCount: 2,
      timeoutCount: 0,
      notReachedCount: 0,
      averageResponseMs: 1200,
      finishedAt: "2026-07-07T08:05:00Z",
      closedAt: "2026-07-07T08:06:00Z",
      releasedAt: "2026-07-07T08:06:10Z",
    }))),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  realtime.options = null;
  showLeaderboard = true;
  roomStatus = "STARTED";
});
afterAll(() => server.close());

describe("StudentLiveQuizPlayPage", () => {
  it("renders the finished leaderboard when enabled", async () => {
    renderPage();

    expect(await screen.findByText("Bảng xếp hạng tạm thời")).toBeInTheDocument();
    expect(screen.getAllByText("Student One").length).toBeGreaterThan(0);
    expect(screen.getByText("Live")).toBeInTheDocument();
  });

  it("keeps the simple waiting screen when leaderboard is disabled", async () => {
    showLeaderboard = false;

    renderPage();

    expect(await screen.findByText("Đã hoàn thành")).toBeInTheDocument();
    expect(screen.queryByText("Bảng xếp hạng tạm thời")).not.toBeInTheDocument();
  });

  it("switches to finalizing state from ROOM_CLOSED realtime message", async () => {
    renderPage();

    expect(await screen.findByText("Đã hoàn thành")).toBeInTheDocument();
    expect(realtime.options).not.toBeNull();

    act(() => {
      realtime.options?.onMessage({
        roomId: "room-1",
        examId: "exam-1",
        type: "ROOM_CLOSED",
        occurredAt: "2026-07-07T08:06:00Z",
        roomStatus: "CLOSED",
        studentTargetId: null,
        participant: null,
        leaderboard,
        summary: null,
      });
    });

    expect(await screen.findByText("Đang chốt kết quả")).toBeInTheDocument();
  });
});

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/student/live-quizzes/room-1/play"]}>
        <Routes>
          <Route path="/student/live-quizzes/:roomId/play" element={<StudentLiveQuizPlayPage />} />
          <Route path="/student/live-quizzes/:roomId/lobby" element={<div>Lobby</div>} />
          <Route path="/student/live-quizzes/:roomId/result" element={<div>Result</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

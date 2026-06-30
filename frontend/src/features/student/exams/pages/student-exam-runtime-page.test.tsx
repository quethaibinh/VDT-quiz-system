import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, expect, test, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { StudentExamRuntimePage } from "@/features/student/exams/pages/student-exam-runtime-page";

const envelope = <T,>(data: T) => ({
  timestamp: "2026-07-01T08:00:00Z",
  status: 200,
  message: "Success",
  data,
});

const requestOrder: string[] = [];
let submitRequests = 0;
let submitBody: unknown;

const answer = { questionId: "q1", selectedOptionIds: [], answerText: null, markedForReview: false };

const server = setupServer(
  http.get("*/v1/api/exam-service/student/exams/:examId", () =>
    HttpResponse.json(envelope({
      examId: "exam-1",
      code: "EX001",
      title: "Practice Exam",
      description: "",
      subjectId: "subject-1",
      subjectName: "Math",
      startAt: "2026-07-01T08:00:00Z",
      endAt: "2026-07-01T09:00:00Z",
      durationMinutes: 60,
      questionCount: 1,
      status: "ACTIVE",
      studentAvailability: "OPEN",
      assignmentStatus: "ASSIGNED",
    }))),
  http.post("*/v1/api/examruntime-service/student/exams/:examId/join", () =>
    HttpResponse.json(envelope({
      sessionId: "session-1",
      examId: "exam-1",
      status: "IN_PROGRESS",
      serverTime: "2026-07-01T08:00:00Z",
      startAt: "2026-07-01T08:00:00Z",
      endAt: "2026-07-01T09:00:00Z",
      canStart: true,
      remainingSecondsToStart: 0,
    }))),
  http.get("*/v1/api/examruntime-service/student/sessions/:sessionId", () =>
    HttpResponse.json(envelope({
      sessionId: "session-1",
      status: "IN_PROGRESS",
      serverStartedAt: "2026-07-01T08:00:00Z",
      serverDeadlineAt: "2026-07-01T09:00:00Z",
      questions: [{
        questionId: "q1",
        difficulty: "EASY",
        type: "SINGLE_CHOICE",
        content: "Question one",
        contentFormat: "TEXT",
        score: 1,
        options: [
          { optionId: "o1", key: "A", content: "Alpha", contentFormat: "TEXT" },
          { optionId: "o2", key: "B", content: "Beta", contentFormat: "TEXT" },
        ],
      }],
      answers: [answer],
    }))),
  http.put("*/v1/api/examruntime-service/student/sessions/:sessionId/answers", async () => {
    requestOrder.push("autosave");
    return HttpResponse.json(envelope({
      sessionId: "session-1",
      acceptedSeq: 2,
      serverSeq: 2,
      savedCount: 1,
      skippedCount: 0,
      storeMode: "REDIS",
      lastAutosaveAt: "2026-07-01T08:05:00Z",
    }));
  }),
  http.post("*/v1/api/examruntime-service/student/sessions/:sessionId/submit", async ({ request }) => {
    requestOrder.push("submit");
    submitRequests += 1;
    submitBody = await request.json();
    return HttpResponse.json(envelope({
      submissionId: "submission-1",
      sessionId: "session-1",
      status: "RECEIVED",
      submitReason: "STUDENT",
      submittedAt: "2026-07-01T08:06:00Z",
    }));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  Object.defineProperty(document, "fullscreenElement", { configurable: true, value: null });
  requestOrder.length = 0;
  submitRequests = 0;
  submitBody = undefined;
  vi.restoreAllMocks();
  window.sessionStorage.clear();
});
afterAll(() => server.close());

test("flushes dirty answers before submit and locks editing after acceptance", async () => {
  const user = userEvent.setup();
  renderPage();

  const option = await screen.findByRole("button", { name: /A\. Alpha/ });
  await user.click(option);
  await user.click(screen.getByRole("button", { name: "Nộp bài" }));
  await user.click(screen.getByRole("button", { name: "Xác nhận nộp" }));

  await screen.findByText("Đã ghi nhận bài nộp");
  expect(requestOrder).toEqual(["autosave", "submit"]);
  expect(submitRequests).toBe(1);
  expect(submitBody).toMatchObject({
    clientSeq: 2,
    finalAnswers: [{ questionId: "q1", selectedOptionIds: ["o1"], answerText: null, markedForReview: false }],
  });
  expect((submitBody as { idempotencyKey?: string }).idempotencyKey).toEqual(expect.any(String));
  expect(option).toBeDisabled();
});

test("blocks copy paste and context menu while the runtime page is active", async () => {
  renderPage();
  await screen.findByText("Question one");

  const copy = new Event("copy", { bubbles: true, cancelable: true });
  document.dispatchEvent(copy);
  expect(copy.defaultPrevented).toBe(true);

  const paste = new Event("paste", { bubbles: true, cancelable: true });
  document.dispatchEvent(paste);
  expect(paste.defaultPrevented).toBe(true);

  const contextMenu = new MouseEvent("contextmenu", { bubbles: true, cancelable: true });
  document.dispatchEvent(contextMenu);
  expect(contextMenu.defaultPrevented).toBe(true);
});

test("shows locked dialog and exits to exam list", async () => {
  server.use(
    http.get("*/v1/api/examruntime-service/student/sessions/:sessionId", () =>
      HttpResponse.json(envelope({
        sessionId: "session-1",
        status: "LOCKED",
        serverStartedAt: "2026-07-01T08:00:00Z",
        serverDeadlineAt: "2026-07-01T09:00:00Z",
        questions: [],
        answers: [],
      }))),
  );
  const user = userEvent.setup();

  renderPage();

  await screen.findByText("Bài thi đã bị khóa");
  await user.click(screen.getByRole("button", { name: "Thoát ra màn ca thi" }));

  expect(await screen.findByText("Exam list")).toBeInTheDocument();
});

function renderPage() {
  Object.defineProperty(document, "fullscreenElement", { configurable: true, value: document.documentElement });
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/student/exams/exam-1/runtime"]}>
        <Routes>
          <Route path="/student/exams/:examId/runtime" element={<StudentExamRuntimePage />} />
          <Route path="/student/exams" element={<div>Exam list</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

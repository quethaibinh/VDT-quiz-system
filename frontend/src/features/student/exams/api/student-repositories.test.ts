import { afterAll, afterEach, beforeAll, expect, test } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { studentExamRepository } from "@/features/student/exams/api/student-exam-repository";
import { studentRuntimeRepository } from "@/features/student/exams/api/student-runtime-repository";

const apiResponse = <T,>(data: T) => ({ timestamp: "now", status: 200, message: "Success", data });

let captured: { path?: string; query?: string; body?: unknown } = {};

const examPage = {
  serverTime: "2026-07-01T07:55:00Z",
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

const paper = {
  sessionId: "session-1",
  status: "IN_PROGRESS",
  serverStartedAt: "2026-07-01T08:00:00Z",
  serverDeadlineAt: "2026-07-01T09:00:00Z",
  questions: [],
  answers: [],
};

const server = setupServer(
  http.get("*/v1/api/exam-service/student/exams", ({ request }) => {
    const url = new URL(request.url);
    captured = { path: url.pathname, query: url.search };
    return HttpResponse.json(apiResponse(examPage));
  }),
  http.get("*/v1/api/exam-service/student/exams/:examId", ({ request }) => {
    captured = { path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse({
      examId: "exam-1",
      code: "EX001",
      title: "Giua ky",
      description: "",
      subjectId: "subject-1",
      subjectName: "Triet hoc",
      startAt: "2026-07-01T08:00:00Z",
      endAt: "2026-07-01T09:00:00Z",
      durationMinutes: 60,
      questionCount: 50,
      status: "SCHEDULED",
      studentAvailability: "UPCOMING",
      assignmentStatus: "ASSIGNED",
    }));
  }),
  http.post("*/v1/api/examruntime-service/student/exams/:examId/join", ({ request }) => {
    captured = { path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse({
      sessionId: "session-1",
      examId: "exam-1",
      status: "CREATED",
      serverTime: "2026-07-01T07:55:00Z",
      startAt: "2026-07-01T08:00:00Z",
      endAt: "2026-07-01T09:00:00Z",
      canStart: false,
      remainingSecondsToStart: 300,
    }));
  }),
  http.post("*/v1/api/examruntime-service/student/exams/:examId/start", ({ request }) => {
    captured = { path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse(paper));
  }),
  http.get("*/v1/api/examruntime-service/student/sessions/:sessionId", ({ request }) => {
    captured = { path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse(paper));
  }),
  http.put("*/v1/api/examruntime-service/student/sessions/:sessionId/answers", async ({ request }) => {
    captured = { path: new URL(request.url).pathname, body: await request.json() };
    return HttpResponse.json(apiResponse({
      sessionId: "session-1",
      acceptedSeq: 12,
      serverSeq: 5,
      savedCount: 1,
      skippedCount: 0,
      storeMode: "REDIS",
      lastAutosaveAt: "2026-07-01T08:15:00Z",
    }));
  }),
  http.post("*/v1/api/examruntime-service/student/sessions/:sessionId/submit", async ({ request }) => {
    captured = { path: new URL(request.url).pathname, body: await request.json() };
    return HttpResponse.json(apiResponse({
      submissionId: "submission-1",
      sessionId: "session-1",
      status: "RECEIVED",
      submitReason: "STUDENT",
      submittedAt: "2026-07-01T08:30:00Z",
    }));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => { server.resetHandlers(); captured = {}; });
afterAll(() => server.close());

test("student exam repository uses exact exam-service paths", async () => {
  await expect(studentExamRepository.listStudentExams({ status: "OPEN", page: 0, size: 10 })).resolves.toEqual(examPage);
  expect(captured.path).toBe("/v1/api/exam-service/student/exams");
  expect(captured.query).toBe("?status=OPEN&page=0&size=10");

  await expect(studentExamRepository.getStudentExam("exam-1")).resolves.toMatchObject({ examId: "exam-1" });
  expect(captured.path).toBe("/v1/api/exam-service/student/exams/exam-1");
});

test("student runtime repository uses examruntime-service paths", async () => {
  await studentRuntimeRepository.joinStudentExam("exam-1");
  expect(captured.path).toBe("/v1/api/examruntime-service/student/exams/exam-1/join");

  await studentRuntimeRepository.startStudentExam("exam-1");
  expect(captured.path).toBe("/v1/api/examruntime-service/student/exams/exam-1/start");

  await studentRuntimeRepository.resumeStudentSession("session-1");
  expect(captured.path).toBe("/v1/api/examruntime-service/student/sessions/session-1");

  await studentRuntimeRepository.autosaveStudentAnswers("session-1", {
    clientSeq: 12,
    answers: [{ questionId: "q1", selectedOptionIds: ["o1"], answerText: null, markedForReview: false }],
  });
  expect(captured.path).toBe("/v1/api/examruntime-service/student/sessions/session-1/answers");
  expect(captured.body).toEqual({
    clientSeq: 12,
    answers: [{ questionId: "q1", selectedOptionIds: ["o1"], answerText: null, markedForReview: false }],
  });

  await expect(studentRuntimeRepository.submitStudentSession("session-1", {
    idempotencyKey: "submit-key-1",
    clientSeq: 13,
    finalAnswers: [{ questionId: "q1", selectedOptionIds: ["o1"], answerText: null, markedForReview: false }],
  })).resolves.toMatchObject({
    submissionId: "submission-1",
    status: "RECEIVED",
  });
  expect(captured.path).toBe("/v1/api/examruntime-service/student/sessions/session-1/submit");
  expect(captured.body).toEqual({
    idempotencyKey: "submit-key-1",
    clientSeq: 13,
    finalAnswers: [{ questionId: "q1", selectedOptionIds: ["o1"], answerText: null, markedForReview: false }],
  });
});

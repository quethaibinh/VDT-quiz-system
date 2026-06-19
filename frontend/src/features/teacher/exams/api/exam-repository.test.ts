import { afterAll, afterEach, beforeAll, expect, test } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import {
  cancelExam,
  createExam,
  examKeys,
  getExam,
  listExams,
  updateExam,
} from "@/features/teacher/exams/api/exam-repository";
import type { ExamDraftRequest } from "@/features/teacher/exams/model/exam-contracts";

const apiResponse = <T,>(data: T) => ({ timestamp: "now", status: 200, message: "Success", data });
const page = { content: [], page: 1, size: 5, totalElements: 0, totalPages: 0, first: false, last: true };
const draft: ExamDraftRequest = {
  title: "Giua ky", description: "", collectionId: "collection-1",
  easyCount: 1, mediumCount: 2, hardCount: 3,
  startAt: "2026-06-20T08:00:00+07:00", durationMinutes: 60,
  joinBeforeMinutes: 10, joinAfterMinutes: 5,
  shuffleQuestions: true, shuffleOptions: true, showResultPolicy: "NEVER",
  autoSubmit: true, requireFullscreen: false, maxViolationAllowed: 3,
  handleViolation: "WARN",
};
const detail = {
  id: "exam-1", code: "EX001", subjectId: "subject-1", subjectName: "Toan",
  collectionName: "Bo de", questionCount: 6, assignedCount: 0, status: "DRAFT",
  version: 1, endAt: "2026-06-20T09:00:00+07:00", ...draft,
};

let captured: { path?: string; query?: string; body?: unknown } = {};
const server = setupServer(
  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams", ({ request }) => {
    const url = new URL(request.url);
    captured = { path: url.pathname, query: url.search };
    return HttpResponse.json(apiResponse(page));
  }),
  http.all("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId", async ({ request }) => {
    captured = {
      path: new URL(request.url).pathname,
      body: request.method === "PUT" ? await request.json() : undefined,
    };
    return HttpResponse.json(apiResponse(detail));
  }),
  http.post("*/v1/api/exam-service/teacher/subjects/:subjectId/exams", async ({ request }) => {
    captured = { path: new URL(request.url).pathname, body: await request.json() };
    return HttpResponse.json(apiResponse(detail));
  }),
  http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/cancel", ({ request }) => {
    captured = { path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse({ ...detail, status: "CANCELLED" }));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => { server.resetHandlers(); captured = {}; });
afterAll(() => server.close());

test("lists subject-scoped exams with exact params and unwraps the page", async () => {
  await expect(listExams("subject-1", {
    status: "DRAFT", keyword: "giua", page: 1, size: 5, sort: "updatedAt,desc",
  })).resolves.toEqual(page);
  expect(captured.path).toBe("/v1/api/exam-service/teacher/subjects/subject-1/exams");
  expect(captured.query).toBe("?status=DRAFT&keyword=giua&page=1&size=5&sort=updatedAt,desc");
});

test("gets, creates, updates and cancels drafts on scoped URLs", async () => {
  await expect(getExam("subject-1", "exam-1")).resolves.toEqual(detail);
  expect(captured.path).toContain("/subjects/subject-1/exams/exam-1");

  await createExam("subject-1", draft);
  expect(captured.body).toEqual(draft);
  expect(captured.body).not.toHaveProperty("subjectId");

  await updateExam("subject-1", "exam-1", draft);
  expect(captured.body).toEqual(draft);

  await expect(cancelExam("subject-1", "exam-1")).resolves.toMatchObject({ status: "CANCELLED" });
  expect(captured.path).toBe("/v1/api/exam-service/teacher/subjects/subject-1/exams/exam-1/cancel");
});

test("provides stable subject-scoped query keys", () => {
  expect(examKeys.list("subject-1", { status: "DRAFT" })).toEqual([
    "teacher", "subjects", "subject-1", "exams", "list", { status: "DRAFT" },
  ]);
  expect(examKeys.detail("subject-1", "exam-1")).toEqual([
    "teacher", "subjects", "subject-1", "exams", "detail", "exam-1",
  ]);
});

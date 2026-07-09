import { afterAll, beforeAll, expect, test } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import {
  addAssignments,
  assignmentKeys,
  listAssignments,
  removeAssignment,
} from "@/features/teacher/exams/api/assignment-repository";

const apiResponse = <T,>(data: T) => ({ timestamp: "now", status: 200, message: "Success", data });
const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true };
const result = { requestedCount: 1, assignedCount: 1, unchangedCount: 0, assignments: [] };
let captured: { method?: string; path?: string; query?: string; body?: unknown } = {};
const root = "*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/assignments";
const server = setupServer(
  http.get(root, ({ request }) => {
    const url = new URL(request.url);
    captured = { method: request.method, path: url.pathname, query: url.search };
    return HttpResponse.json(apiResponse(page));
  }),
  http.post(root, async ({ request }) => {
    captured = { method: request.method, path: new URL(request.url).pathname, body: await request.json() };
    return HttpResponse.json(apiResponse(result));
  }),
  http.delete(`${root}/:studentId`, ({ request }) => {
    captured = { method: request.method, path: new URL(request.url).pathname };
    return HttpResponse.json(apiResponse(result));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterAll(() => server.close());

test("lists, adds and removes assignments using exact backend contracts", async () => {
  await expect(listAssignments("subject-1", "exam-1", { page: 0, size: 20 })).resolves.toEqual(page);
  expect(captured.query).toBe("?page=0&size=20");

  await expect(addAssignments("subject-1", "exam-1", ["student-1"])).resolves.toEqual(result);
  expect(captured.body).toEqual({ studentIds: ["student-1"] });

  await expect(removeAssignment("subject-1", "exam-1", "student-1")).resolves.toEqual(result);
  expect(captured.path).toBe(
    "/v1/api/exam-service/teacher/subjects/subject-1/exams/exam-1/assignments/student-1",
  );
  expect(assignmentKeys.list("subject-1", "exam-1")).toEqual([
    "teacher", "subjects", "subject-1", "exams", "detail", "exam-1", "assignments",
  ]);
});

import { afterAll, beforeAll, expect, test } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { searchStudents, studentKeys } from "@/features/teacher/exams/api/student-repository";

let query = "";
const page = { content: [], page: 2, size: 10, totalElements: 0, totalPages: 0, first: false, last: true };
const server = setupServer(http.get("*/v1/api/auth-service/teacher/students", ({ request }) => {
  query = new URL(request.url).search;
  return HttpResponse.json({ timestamp: "now", status: 200, message: "Success", data: page });
}));

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterAll(() => server.close());

test("searches students with exact params and unwraps the page", async () => {
  await expect(searchStudents({ keyword: "SV", page: 2, size: 10, sort: "studentCode,asc" }))
    .resolves.toEqual(page);
  expect(query).toBe("?keyword=SV&page=2&size=10&sort=studentCode,asc");
  expect(studentKeys.search({ keyword: "SV" })).toEqual([
    "teacher", "students", "search", { keyword: "SV" },
  ]);
});

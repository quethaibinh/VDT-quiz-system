import { fromExamTimestamp, toExamTimestamp } from "@/features/teacher/exams/lib/exam-date";
import { expect, test } from "vitest";

test("converts datetime-local to an ISO timestamp with an explicit offset", () => {
  expect(toExamTimestamp("2026-06-20T08:15")).toMatch(
    /^2026-06-20T08:15:00[+-]\d{2}:\d{2}$/,
  );
});

test("round-trips a local datetime without shifting its wall-clock value", () => {
  const localValue = "2026-12-20T08:15";
  expect(fromExamTimestamp(toExamTimestamp(localValue))).toBe(localValue);
});

test("converts backend offset timestamps to datetime-local values", () => {
  const expected = fromExamTimestamp("2026-06-20T01:15:00Z");
  expect(expected).toMatch(/^2026-06-20T\d{2}:15$/);
});

import { describe, expect, it } from "vitest";
import {
  getScheduleDisabledReason,
  getScheduleErrorMessage,
} from "@/features/teacher/exams/lib/exam-scheduling";
import type { ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const exam: ExamSummary = {
  id: "exam-1",
  code: "EX001",
  title: "Thi cuối kỳ",
  subjectId: "subject-1",
  subjectName: "Toán cao cấp",
  collectionId: "collection-1",
  collectionName: "Bộ đề cuối kỳ",
  startAt: "2026-06-22T09:00:00+07:00",
  durationMinutes: 60,
  questionCount: 50,
  assignedCount: 20,
  status: "DRAFT",
  version: 1,
};

describe("getScheduleDisabledReason", () => {
  it("requires at least one assigned student", () => {
    expect(getScheduleDisabledReason(
      { ...exam, assignedCount: 0 },
      new Date("2026-06-21T09:00:00+07:00").getTime(),
    )).toBe("Cần phân công ít nhất 1 học sinh.");
  });

  it("requires a future start time", () => {
    expect(getScheduleDisabledReason(
      { ...exam, startAt: "2026-06-21T08:00:00+07:00" },
      new Date("2026-06-21T09:00:00+07:00").getTime(),
    )).toBe("Thời gian bắt đầu phải ở tương lai.");
  });

  it("allows a future exam with assigned students", () => {
    expect(getScheduleDisabledReason(
      exam,
      new Date("2026-06-21T09:00:00+07:00").getTime(),
    )).toBeNull();
  });

  it("maps backend scheduling codes to actionable Vietnamese messages", () => {
    expect(getScheduleErrorMessage(new Error("EXAM_ASSIGNMENT_REQUIRED")))
      .toBe("Cần phân công ít nhất 1 học sinh trước khi lên lịch.");
    expect(getScheduleErrorMessage(new Error("INSUFFICIENT_COLLECTION_QUOTA")))
      .toBe("Bộ câu hỏi không đủ số lượng theo mức độ đã cấu hình.");
    expect(getScheduleErrorMessage(new Error("QUESTION_SERVICE_UNAVAILABLE")))
      .toBe("Dịch vụ câu hỏi đang tạm thời gián đoạn. Vui lòng thử lại sau.");
  });
});

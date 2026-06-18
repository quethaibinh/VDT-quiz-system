import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ResultExamRow } from "@/features/teacher/results/components/result-exam-row";
import type { ExamSummary } from "@/features/teacher/exams";

const exam: ExamSummary = {
  id: "exam-1",
  title: "Thi cuối kỳ",
  subjectId: "subject-1",
  subjectName: "Toán cao cấp",
  startAt: "2026-06-05T09:00:00+07:00",
  durationMinutes: 60,
  questionCount: 50,
  assignedCount: 100,
  status: "CLOSED",
};

describe("ResultExamRow", () => {
  it("links a closed exam to its result detail", () => {
    render(<MemoryRouter><ResultExamRow exam={exam} backTo="/teacher/subjects/subject-1/results" /></MemoryRouter>);
    expect(screen.getByRole("link", { name: /Mở báo cáo/i })).toHaveAttribute("href", "/teacher/exams/exam-1/results");
  });
});

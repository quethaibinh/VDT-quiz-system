import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { ExamSubjectRow } from "@/features/teacher/exams/components/exam-subject-row";
import { ResultSubjectRow } from "@/features/teacher/results/components/result-subject-row";
import { SubjectCard } from "@/features/teacher/subjects/components/subject-card";
import type { Subject } from "@/features/teacher/subjects";

const subject: Subject = {
  id: "subject-1",
  code: "MAT101",
  name: "Toán cao cấp",
  description: "Môn học thử nghiệm",
  status: "ACTIVE",
  createdAt: "2026-06-11T00:00:00Z",
  updatedAt: "2026-06-11T00:00:00Z",
};

describe("subject journey entry components", () => {
  it("uses a workspace card for subject management", () => {
    render(<MemoryRouter><SubjectCard subject={subject} /></MemoryRouter>);
    expect(screen.getByRole("link", { name: /Mở không gian môn/i })).toHaveAttribute("href", "/teacher/subjects/subject-1");
  });

  it("uses an exam-oriented row and route", () => {
    render(<MemoryRouter><ExamSubjectRow subject={subject} /></MemoryRouter>);
    expect(screen.getByRole("link", { name: /Xem ca thi/i })).toHaveAttribute("href", "/teacher/subjects/subject-1/exams");
  });

  it("uses a result-oriented row and route", () => {
    render(<MemoryRouter><ResultSubjectRow subject={subject} /></MemoryRouter>);
    expect(screen.getByText(/Báo cáo môn học/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Xem kết quả/i })).toHaveAttribute("href", "/teacher/subjects/subject-1/results");
  });
});

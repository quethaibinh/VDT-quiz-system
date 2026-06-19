import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ExamListItem } from "./exam-list-item";
import type { ExamStatus, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const makeExam = (status: ExamStatus): ExamSummary => ({
  id: "exam-1",
  code: "EX001",
  title: "Thi cuối kỳ",
  subjectId: "subject-1",
  subjectName: "Toán cao cấp",
  collectionId: "collection-1",
  collectionName: "Bộ đề cuối kỳ",
  startAt: "2026-06-20T09:00:00+07:00",
  durationMinutes: 60,
  questionCount: 50,
  assignedCount: 20,
  status,
  version: 1,
});

describe("ExamListItem", () => {
  afterEach(cleanup);

  it("shows edit and cancel actions only for drafts", async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();
    render(
      <MemoryRouter>
        <ExamListItem exam={makeExam("DRAFT")} onCancel={onCancel} />
      </MemoryRouter>,
    );

    expect(screen.getByRole("link", { name: "Chỉnh sửa" })).toHaveAttribute(
      "href",
      "/teacher/subjects/subject-1/exams/exam-1/edit",
    );
    await user.click(screen.getByRole("button", { name: "Hủy ca thi" }));
    expect(onCancel).toHaveBeenCalledWith("exam-1");
    expect(screen.queryByText(/kích hoạt|giám sát|kết quả/i)).not.toBeInTheDocument();
  });

  it.each([
    ["SCHEDULED", "Chờ bắt đầu"],
    ["ACTIVE", "Đang diễn ra"],
    ["CLOSED", "Đã kết thúc"],
    ["CANCELLED", "Đã hủy"],
  ] as const)("shows %s without unsupported actions", (status, label) => {
    render(
      <MemoryRouter>
        <ExamListItem exam={makeExam(status)} onCancel={vi.fn()} />
      </MemoryRouter>,
    );

    expect(screen.getByText(label)).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Chỉnh sửa" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Hủy ca thi" })).not.toBeInTheDocument();
    expect(screen.queryByText(/kích hoạt|giám sát|kết quả/i)).not.toBeInTheDocument();
  });
});

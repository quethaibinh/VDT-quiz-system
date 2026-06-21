import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from "vitest";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { SubjectExamListPage } from "@/features/teacher/exams/pages/exam-list-page";
import type { ExamDetail, ExamSummary } from "@/features/teacher/exams/model/exam-contracts";

const envelope = <T,>(data: T) => ({
  timestamp: "2026-06-21T09:00:00Z",
  status: 200,
  message: "Success",
  data,
});

let exam: ExamDetail;
let secondExam: ExamDetail | null;
let detailRequests = 0;
let scheduleRequests = 0;

const server = setupServer(
  http.get("*/v1/api/question-service/teacher/subjects/:subjectId", () =>
    HttpResponse.json(envelope({
      id: "subject-1",
      code: "MATH",
      name: "Toán cao cấp",
      status: "ACTIVE",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    }))),
  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams", () =>
    HttpResponse.json(envelope({
      content: [toSummary(exam), ...(secondExam ? [toSummary(secondExam)] : [])],
      page: 0,
      size: 10,
      totalElements: secondExam ? 2 : 1,
      totalPages: 1,
      first: true,
      last: true,
    }))),
  http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId", ({ params }) => {
    detailRequests += 1;
    return HttpResponse.json(envelope(
      params.examId === secondExam?.id ? secondExam : exam,
    ));
  }),
  http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/schedule", () => {
    scheduleRequests += 1;
    exam = { ...exam, status: "SCHEDULED", version: exam.version + 1 };
    return HttpResponse.json(envelope(exam));
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
beforeEach(() => {
  detailRequests = 0;
  scheduleRequests = 0;
  exam = makeExam();
  secondExam = null;
});
afterEach(() => {
  cleanup();
  server.resetHandlers();
});
afterAll(() => server.close());

describe("SubjectExamListPage", () => {
  it("loads details only after the card is expanded", async () => {
    const user = userEvent.setup();
    renderPage();

    const trigger = await screen.findByRole("button", { name: /Thi cuối kỳ/ });
    expect(detailRequests).toBe(0);

    await user.click(trigger);
    expect(await screen.findByRole("region", { name: /Thi cuối kỳ/ })).toBeInTheDocument();
    expect(await screen.findByText("Bộ đề cuối kỳ")).toBeInTheDocument();
    expect(detailRequests).toBe(1);

    await user.click(trigger);
    expect(screen.queryByRole("region", { name: /Thi cuối kỳ/ })).not.toBeInTheDocument();
  });

  it("keeps only one exam expanded and reuses fresh cached details", async () => {
    const user = userEvent.setup();
    secondExam = { ...makeExam(), id: "exam-2", title: "Thi giữa kỳ", code: "EX002" };
    renderPage();

    const first = await screen.findByRole("button", { name: /Thi cuối kỳ/ });
    const second = screen.getByRole("button", { name: /Thi giữa kỳ/ });
    await user.click(first);
    await screen.findByRole("region", { name: /Thi cuối kỳ/ });
    await user.click(second);

    expect(screen.queryByRole("region", { name: /Thi cuối kỳ/ })).not.toBeInTheDocument();
    expect(await screen.findByRole("region", { name: /Thi giữa kỳ/ })).toBeInTheDocument();

    await user.click(first);
    expect(await screen.findByRole("region", { name: /Thi cuối kỳ/ })).toBeInTheDocument();
    expect(detailRequests).toBe(2);
  });

  it("keeps the panel open and retries a failed detail request", async () => {
    const user = userEvent.setup();
    let attempts = 0;
    server.use(
      http.get("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId", () => {
        attempts += 1;
        if (attempts === 1) {
          return HttpResponse.json({ status: 500, message: "Không tải được chi tiết." }, { status: 500 });
        }
        return HttpResponse.json(envelope({ ...exam, description: null }));
      }),
    );
    renderPage();

    await user.click(await screen.findByRole("button", { name: /Thi cuối kỳ/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent("Không tải được chi tiết.");
    await user.click(screen.getByRole("button", { name: "Thử lại" }));

    expect(await screen.findByText("Ca thi không có mô tả.")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: /Thi cuối kỳ/ })).toBeInTheDocument();
  });

  it("confirms scheduling and refreshes the card status without navigation", async () => {
    const user = userEvent.setup();
    renderPage("/teacher/subjects/subject-1/exams?keyword=cuoi&status=DRAFT&page=2");

    await user.click(await screen.findByRole("button", { name: "Lên lịch" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("50 câu · 20 học sinh");
    expect(screen.getByRole("dialog")).toHaveTextContent("không thể chỉnh sửa cấu hình");

    await user.click(screen.getByRole("button", { name: "Xác nhận lên lịch" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Đã lên lịch ca thi");
    await waitFor(() => {
      const card = screen.getByRole("button", { name: /Thi cuối kỳ/ }).closest("article");
      expect(card).not.toBeNull();
      expect(within(card as HTMLElement).getByText("Chờ bắt đầu")).toBeInTheDocument();
    });
    expect(scheduleRequests).toBe(1);
    expect(screen.queryByRole("button", { name: "Lên lịch" })).not.toBeInTheDocument();
    expect(screen.getByLabelText("current-location"))
      .toHaveTextContent("?keyword=cuoi&status=DRAFT&page=2");
  });

  it("disables scheduling and explains when no student is assigned", async () => {
    exam = { ...makeExam(), assignedCount: 0 };
    renderPage();

    const button = await screen.findByRole("button", { name: "Lên lịch" });
    expect(button).toBeDisabled();
    expect(screen.getByText("Cần phân công ít nhất 1 học sinh.")).toBeInTheDocument();
  });

  it("disables scheduling and explains when the start time has passed", async () => {
    exam = { ...makeExam(), startAt: "2020-01-01T08:00:00+07:00" };
    renderPage();

    expect(await screen.findByRole("button", { name: "Lên lịch" })).toBeDisabled();
    expect(screen.getByText("Thời gian bắt đầu phải ở tương lai.")).toBeInTheDocument();
  });

  it("keeps the draft and shows a mapped message when scheduling is rejected", async () => {
    const user = userEvent.setup();
    server.use(
      http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/schedule", () =>
        HttpResponse.json({ status: 409, message: "EXAM_ASSIGNMENT_REQUIRED" }, { status: 409 })),
    );
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Lên lịch" }));
    await user.click(screen.getByRole("button", { name: "Xác nhận lên lịch" }));

    expect(await screen.findByRole("alert"))
      .toHaveTextContent("Cần phân công ít nhất 1 học sinh trước khi lên lịch.");
    await user.click(screen.getByRole("button", { name: "Hủy" }));
    const card = screen.getByRole("button", { name: /Thi cuối kỳ/ }).closest("article");
    expect(card).not.toBeNull();
    expect(within(card as HTMLElement).getByText("Bản nháp")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Lên lịch" })).toBeInTheDocument();
  });

  it("prevents duplicate scheduling submissions while pending", async () => {
    const user = userEvent.setup();
    server.use(
      http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/schedule", async () => {
        scheduleRequests += 1;
        await delay(100);
        exam = { ...exam, status: "SCHEDULED" };
        return HttpResponse.json(envelope(exam));
      }),
    );
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Lên lịch" }));
    const confirm = screen.getByRole("button", { name: "Xác nhận lên lịch" });
    await user.dblClick(confirm);

    await screen.findByText("Đã lên lịch ca thi “Thi cuối kỳ”.");
    expect(scheduleRequests).toBe(1);
  });

  it.each([
    ["404", "Không tìm thấy ca thi"],
    ["network", "Không thể kết nối đến máy chủ"],
  ] as const)("keeps the draft after a %s scheduling failure", async (failureType, message) => {
    const user = userEvent.setup();
    server.use(
      http.patch("*/v1/api/exam-service/teacher/subjects/:subjectId/exams/:examId/schedule", () => {
        if (failureType === "network") return HttpResponse.error();
        return HttpResponse.json(
          { status: 404, message: "EXAM_NOT_FOUND" },
          { status: 404 },
        );
      }),
    );
    renderPage();

    await user.click(await screen.findByRole("button", { name: "Lên lịch" }));
    await user.click(screen.getByRole("button", { name: "Xác nhận lên lịch" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(message);
    expect(scheduleRequests).toBe(0);
  });
});

function renderPage(initialEntry = "/teacher/subjects/subject-1/exams") {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 30_000 },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/teacher/subjects/:subjectId/exams" element={<SubjectExamListPage />} />
        </Routes>
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function LocationProbe() {
  const location = useLocation();
  return <span aria-label="current-location">{location.search}</span>;
}

function makeExam(): ExamDetail {
  return {
    id: "exam-1",
    code: "EX001",
    title: "Thi cuối kỳ",
    description: "Kiểm tra kiến thức học kỳ.",
    subjectId: "subject-1",
    subjectName: "Toán cao cấp",
    collectionId: "collection-1",
    collectionName: "Bộ đề cuối kỳ",
    easyCount: 20,
    mediumCount: 20,
    hardCount: 10,
    startAt: "2027-06-22T09:00:00+07:00",
    endAt: "2027-06-22T10:00:00+07:00",
    durationMinutes: 60,
    joinBeforeMinutes: 10,
    joinAfterMinutes: 5,
    shuffleQuestions: true,
    shuffleOptions: true,
    showResultPolicy: "AFTER_CLOSED",
    autoSubmit: true,
    requireFullscreen: true,
    maxViolationAllowed: 3,
    handleViolation: "WARN",
    assignedCount: 20,
    status: "DRAFT",
    version: 1,
  };
}

function toSummary(value: ExamDetail): ExamSummary {
  return {
    id: value.id,
    code: value.code,
    title: value.title,
    subjectId: value.subjectId,
    subjectName: value.subjectName,
    collectionId: value.collectionId,
    collectionName: value.collectionName,
    startAt: value.startAt,
    durationMinutes: value.durationMinutes,
    questionCount: value.easyCount + value.mediumCount + value.hardCount,
    assignedCount: value.assignedCount,
    status: value.status,
    version: value.version,
  };
}

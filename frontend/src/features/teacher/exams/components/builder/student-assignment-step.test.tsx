import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor, within, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactNode } from "react";
import { useState } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { searchStudents } from "@/features/teacher/exams/api/student-repository";
import type { StudentSummary } from "@/features/teacher/exams/model/exam-contracts";
import { StudentAssignmentStep } from "./student-assignment-step";

let observerCallback: IntersectionObserverCallback | null = null;

class IntersectionObserverMock {
  constructor(callback: IntersectionObserverCallback) {
    observerCallback = callback;
  }
  observe() {}
  unobserve() {}
  disconnect() {}
}
vi.stubGlobal("IntersectionObserver", IntersectionObserverMock);

vi.mock("@/features/teacher/exams/api/student-repository", () => ({
  searchStudents: vi.fn(),
}));

const students: StudentSummary[] = [
  { id: "student-1", studentCode: "SV001", fullName: "Nguyễn An", displayName: "An" },
  { id: "student-2", studentCode: "SV002", fullName: "Trần Bình", displayName: "Bình" },
];

const page = (content: StudentSummary[], currentPage = 0, totalPages = 1) => ({
  content,
  page: currentPage,
  size: 20,
  totalElements: content.length,
  totalPages,
  first: currentPage === 0,
  last: currentPage === totalPages - 1,
});

function renderStep(initial = new Map<string, StudentSummary>()) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });

  function ControlledStep() {
    const [selected, setSelected] = useState(initial);
    return <StudentAssignmentStep selected={selected} onChange={setSelected} />;
  }

  const Wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return render(<ControlledStep />, { wrapper: Wrapper });
}

describe("StudentAssignmentStep", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.mocked(searchStudents).mockReset();
  });

  it("searches, paginates, and preserves selection across result changes", async () => {
    const user = userEvent.setup();
    vi.mocked(searchStudents).mockImplementation(async (params = {}) => {
      const { keyword, page: currentPage } = params;
      if (keyword === "không có") return page([], 0, 1);
      if (currentPage === 1) return page([students[1]], 1, 2);
      return page([students[0]], 0, 2);
    });

    renderStep();

    await user.click(await screen.findByRole("checkbox", { name: /An/ }));
    expect(screen.getByRole("button", { name: "Bỏ chọn An" })).toBeInTheDocument();

    // Kich hoat IntersectionObserver de trigger fetchNextPage
    expect(observerCallback).not.toBeNull();
    await act(async () => {
      observerCallback!(
        [{ isIntersecting: true } as unknown as IntersectionObserverEntry],
        {} as unknown as IntersectionObserver
      );
    });

    expect(await screen.findByRole("checkbox", { name: /Bình/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Bỏ chọn An" })).toBeInTheDocument();

    await user.type(screen.getByRole("searchbox", { name: /tìm theo tên/i }), "không có");
    await waitFor(() =>
      expect(searchStudents).toHaveBeenLastCalledWith(
        expect.objectContaining({ keyword: "không có", page: 0 }),
      ),
    );
    expect(await screen.findByText("Không tìm thấy học sinh phù hợp.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Bỏ chọn An" })).toBeInTheDocument();
  });

  it("removes selected students and shows the empty-selection validation", async () => {
    const user = userEvent.setup();
    vi.mocked(searchStudents).mockResolvedValue(page(students));
    renderStep(new Map([[students[0].id, students[0]]]));

    await user.click(screen.getByRole("button", { name: "Bỏ chọn An" }));

    expect(screen.getByText("Chưa chọn học sinh nào.")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Chọn ít nhất một học sinh");
  });

  it("keeps selections when search fails and exposes a retry action", async () => {
    vi.mocked(searchStudents).mockRejectedValue(new Error("Mất kết nối"));
    renderStep(new Map([[students[0].id, students[0]]]));

    const alert = await screen.findByRole("alert");
    expect(within(alert).getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Bỏ chọn An" })).toBeInTheDocument();
  });

  it("enforces the 100-student limit without a select-all control", async () => {
    const selected = new Map(
      Array.from({ length: 100 }, (_, index) => {
        const student = {
          id: `selected-${index}`,
          studentCode: `SV${index}`,
          fullName: `Học sinh ${index}`,
          displayName: `Học sinh ${index}`,
        };
        return [student.id, student] as const;
      }),
    );
    vi.mocked(searchStudents).mockResolvedValue(page([students[1]]));
    renderStep(selected);

    expect(await screen.findByRole("checkbox", { name: /Bình/ })).toBeDisabled();
    expect(screen.getByText(/giới hạn 100 học sinh/i)).toBeInTheDocument();
    expect(screen.queryByRole("checkbox", { name: /chọn tất cả/i })).not.toBeInTheDocument();
  });
});

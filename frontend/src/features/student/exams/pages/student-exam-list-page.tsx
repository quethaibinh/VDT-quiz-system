import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { studentExamRepository } from "../api/student-exam-repository";
import { StudentExamCard } from "../components/student-exam-card";
import type { StudentExamAvailability } from "../model/student-exam-contracts";

const PAGE_SIZE = 9;

// Man hinh danh sach ca thi cua hoc sinh
export function StudentExamListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(0, Number(searchParams.get("page") ?? 0));
  const statusFilter = (searchParams.get("status") ?? "") as StudentExamAvailability | "";

  const query = useQuery({
    queryKey: ["student", "exams", { status: statusFilter, page, size: PAGE_SIZE }],
    queryFn: () =>
      studentExamRepository.listStudentExams({
        status: statusFilter || undefined,
        page,
        size: PAGE_SIZE,
      }),
  });

  const handleTabChange = (status: StudentExamAvailability | "") => {
    const nextParams = new URLSearchParams(searchParams);
    if (status) nextParams.set("status", status);
    else nextParams.delete("status");
    nextParams.delete("page");
    setSearchParams(nextParams);
  };

  const handlePageChange = (nextPage: number) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set("page", String(nextPage));
    setSearchParams(nextParams);
  };

  const tabs: { label: string; value: StudentExamAvailability | "" }[] = [
    { label: "Tất cả ca thi", value: "" },
    { label: "Đang mở", value: "OPEN" },
    { label: "Sắp diễn ra", value: "UPCOMING" },
    { label: "Đã kết thúc", value: "ENDED" },
  ];

  return (
    <div className="space-y-6 p-4 md:p-6">
      <PageHeader
        title="Ca thi của tôi"
        description="Xem và tham gia các ca thi bạn được phân công làm bài."
      />

      <div className="flex gap-2 overflow-x-auto border-b border-line pb-px">
        {tabs.map((tab) => {
          const isActive = statusFilter === tab.value;
          return (
            <button
              key={tab.value}
              type="button"
              onClick={() => handleTabChange(tab.value)}
              className={`-mb-px min-h-10 whitespace-nowrap border-b-2 px-4 py-2 text-sm font-semibold transition ${
                isActive
                  ? "border-primary text-primary"
                  : "border-transparent text-muted hover:text-ink"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      <DataState
        loading={query.isLoading}
        error={query.error ? "Không thể tải danh sách ca thi. Vui lòng thử lại." : null}
        empty={query.data?.content.length === 0}
        emptyMessage="Không có ca thi nào trong danh mục này."
        onRetry={() => query.refetch()}
      >
        {query.data && (
          <div className="space-y-6">
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {query.data.content.map((exam) => (
                <StudentExamCard key={exam.examId} exam={exam} />
              ))}
            </div>

            {query.data.totalPages > 1 && (
              <Pagination
                page={query.data.page}
                totalPages={query.data.totalPages}
                onChange={handlePageChange}
              />
            )}
          </div>
        )}
      </DataState>
    </div>
  );
}

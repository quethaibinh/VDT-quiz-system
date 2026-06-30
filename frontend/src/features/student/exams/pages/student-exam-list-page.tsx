import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, BookOpen, ChevronRight } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Button } from "@/components/ui/button";
import { buildStudentSubjectGroups, type StudentSubjectGroup } from "@/features/student/shared/student-subject-groups";
import { studentExamRepository } from "../api/student-exam-repository";
import { StudentExamCard } from "../components/student-exam-card";
import type { StudentExamAvailability } from "../model/student-exam-contracts";

const PAGE_SIZE = 9;
const SUBJECT_PICKER_SIZE = 100;

export function StudentExamListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedSubjectId = searchParams.get("subjectId") ?? "";
  const page = Math.max(0, Number(searchParams.get("page") ?? 0));
  const statusFilter = (searchParams.get("status") ?? "") as StudentExamAvailability | "";
  const isDrilldown = Boolean(selectedSubjectId);

  const query = useQuery({
    queryKey: ["student", "exams", { status: isDrilldown ? statusFilter : "", page: isDrilldown ? page : 0, size: isDrilldown ? PAGE_SIZE : SUBJECT_PICKER_SIZE }],
    queryFn: () =>
      studentExamRepository.listStudentExams({
        status: isDrilldown && statusFilter ? statusFilter : undefined,
        page: isDrilldown ? page : 0,
        size: isDrilldown ? PAGE_SIZE : SUBJECT_PICKER_SIZE,
      }),
  });

  const exams = query.data?.content ?? [];
  const subjectGroups = useMemo(() => buildStudentSubjectGroups(exams), [exams]);
  const selectedSubject = subjectGroups.find((subject) => subject.subjectId === selectedSubjectId);
  const visibleExams = isDrilldown ? exams.filter((exam) => exam.subjectId === selectedSubjectId) : exams;

  const handleSubjectSelect = (subjectId: string) => {
    const nextParams = new URLSearchParams();
    nextParams.set("subjectId", subjectId);
    setSearchParams(nextParams);
  };

  const handleBackToSubjects = () => {
    setSearchParams({});
  };

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
        title={selectedSubject ? `Ca thi · ${selectedSubject.subjectName}` : isDrilldown ? "Ca thi" : "Ca thi của tôi"}
        description={isDrilldown ? "Danh sách ca thi của môn học đã chọn." : "Chọn môn học để xem các ca thi bạn được phân công."}
        action={isDrilldown ? <Button variant="secondary" onClick={handleBackToSubjects}><ArrowLeft size={16} />Đổi môn</Button> : undefined}
      />

      <DataState
        loading={query.isLoading}
        error={query.error ? "Không thể tải danh sách ca thi. Vui lòng thử lại." : null}
        empty={query.isSuccess && (isDrilldown ? visibleExams.length === 0 : exams.length === 0)}
        emptyMessage={isDrilldown ? "Không có ca thi nào trong danh mục này." : "Bạn chưa có ca thi được phân công."}
        onRetry={() => query.refetch()}
      >
        {!isDrilldown ? (
          <SubjectPicker subjects={subjectGroups} onSelect={handleSubjectSelect} />
        ) : (
          <div className="space-y-6">
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

            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {visibleExams.map((exam) => (
                <StudentExamCard key={exam.examId} exam={exam} />
              ))}
            </div>

            {query.data && query.data.totalPages > 1 && (
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

function SubjectPicker({ subjects, onSelect }: { subjects: StudentSubjectGroup[]; onSelect: (subjectId: string) => void }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {subjects.map((subject) => (
        <button
          key={subject.subjectId}
          type="button"
          onClick={() => onSelect(subject.subjectId)}
          className="group flex min-h-32 items-center justify-between gap-4 rounded-xl border border-line bg-surface p-5 text-left shadow-soft transition hover:border-primary/40 hover:shadow-md"
        >
          <span className="flex min-w-0 items-center gap-3">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-primary/10 text-primary">
              <BookOpen size={22} />
            </span>
            <span className="min-w-0">
              <span className="block truncate text-lg font-bold text-ink">{subject.subjectName}</span>
              <span className="mt-1 block text-sm font-medium text-muted">{subject.total} ca thi</span>
            </span>
          </span>
          <ChevronRight size={20} className="shrink-0 text-muted transition group-hover:text-primary" />
        </button>
      ))}
    </div>
  );
}

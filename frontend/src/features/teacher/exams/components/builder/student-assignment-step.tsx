import { useInfiniteQuery } from "@tanstack/react-query";
import { useEffect, useId, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { searchStudents } from "@/features/teacher/exams/api/student-repository";
import type { StudentSummary } from "@/features/teacher/exams/model/exam-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

const PAGE_SIZE = 20;
const MAX_SELECTED_STUDENTS = 100;

interface StudentAssignmentStepProps {
  selected: ReadonlyMap<string, StudentSummary>;
  onChange: (selected: Map<string, StudentSummary>) => void;
  validationError?: string;
}

export function StudentAssignmentStep({
  selected,
  onChange,
  validationError,
}: StudentAssignmentStepProps) {
  const searchId = useId();
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");

  // Ref container cuon va ref sentinel o day danh sach
  const containerRef = useRef<HTMLDivElement>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedKeyword(keyword.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [keyword]);

  // Cuon ve top khi thay doi tu khoa tim kiem
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = 0;
    }
  }, [debouncedKeyword]);

  // Dung useInfiniteQuery de quan ly phan trang tu dong qua cuon vo han
  const students = useInfiniteQuery({
    queryKey: ["teacher", "students", debouncedKeyword, PAGE_SIZE],
    queryFn: ({ pageParam = 0 }) =>
      searchStudents({
        keyword: debouncedKeyword,
        page: pageParam,
        size: PAGE_SIZE,
        sort: "studentCode,asc",
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.page < lastPage.totalPages - 1 ? lastPage.page + 1 : undefined,
  });

  const { hasNextPage, isFetchingNextPage, fetchNextPage } = students;

  // Lang nghe giao cat de tu dong tai trang tiep theo khi cuon den đáy
  useEffect(() => {
    const container = containerRef.current;
    const sentinel = sentinelRef.current;
    if (!container || !sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const first = entries[0];
        if (first.isIntersecting && hasNextPage && !isFetchingNextPage) {
          void fetchNextPage();
        }
      },
      {
        root: container,
        threshold: 0.1,
      }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const updateSelection = (student: StudentSummary, checked: boolean) => {
    const next = new Map(selected);
    if (checked) {
      if (next.size >= MAX_SELECTED_STUDENTS) return;
      next.set(student.id, student);
    } else {
      next.delete(student.id);
    }
    onChange(next);
  };

  const atLimit = selected.size >= MAX_SELECTED_STUDENTS;

  // Gop tat ca cac trang hoc sinh lai thanh mot mang phang
  const allStudents = students.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="mt-0">Phân công học sinh</h2>
        <label htmlFor={searchId} className="block text-sm font-semibold">
          Tìm theo tên hoặc mã học sinh
        </label>
        <Input
          id={searchId}
          className="mt-2"
          type="search"
          value={keyword}
          placeholder="Nhập tên hoặc mã học sinh"
          onChange={(event) => {
            setKeyword(event.target.value);
          }}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-4">
        {/* Cot trai: Ket qua tim kiem */}
        <section aria-labelledby={`${searchId}-results`} className="space-y-3">
          <div className="flex items-center justify-between gap-3 h-9">
            <h3 id={`${searchId}-results`} className="m-0 text-lg font-semibold">
              Kết quả tìm kiếm
            </h3>
            {students.isFetching && (
              <span role="status" className="text-sm text-muted">
                Đang tìm học sinh...
              </span>
            )}
          </div>

          {students.isError && (
            <div role="alert" className="rounded-lg border border-danger/30 p-4 text-danger h-[300px] flex flex-col justify-center items-center bg-danger/5">
              <p className="m-0 text-center">{getApiErrorMessage(students.error)}</p>
              <Button className="mt-3" variant="secondary" onClick={() => void students.refetch()}>
                Thử lại
              </Button>
            </div>
          )}

          {students.isSuccess && allStudents.length === 0 && (
            <div role="status" className="rounded-lg bg-canvas border border-line p-4 text-muted h-[300px] flex items-center justify-center">
              Không tìm thấy học sinh phù hợp.
            </div>
          )}

          {allStudents.length > 0 && (
            <div className="space-y-3">
              <div ref={containerRef} className="max-h-[300px] overflow-y-auto border border-line rounded-lg bg-canvas">
                <ul className="m-0 divide-y divide-line p-0">
                  {allStudents.map((student) => {
                    const isSelected = selected.has(student.id);
                    const displayName = student.displayName || student.fullName;
                    return (
                      <li key={student.id} className="flex items-center gap-3 p-3 hover:bg-primary/5 transition-colors">
                        <input
                          id={`${searchId}-${student.id}`}
                          type="checkbox"
                          checked={isSelected}
                          disabled={!isSelected && atLimit}
                          onChange={(event) => updateSelection(student, event.target.checked)}
                          className="h-4 w-4"
                        />
                        <label htmlFor={`${searchId}-${student.id}`} className="min-w-0 flex-1 cursor-pointer">
                          <strong className="block truncate text-sm">{displayName}</strong>
                          <span className="text-xs text-muted">
                            {student.studentCode}
                            {student.fullName !== displayName ? ` · ${student.fullName}` : ""}
                          </span>
                        </label>
                      </li>
                    );
                  })}
                </ul>
                {/* Sentinel phat hien cuon cham day */}
                <div ref={sentinelRef} className="p-3 text-center border-t border-line/50 flex items-center justify-center">
                  {isFetchingNextPage ? (
                    <span className="text-xs text-muted">Đang tải thêm...</span>
                  ) : hasNextPage ? (
                    <span className="text-xs text-muted/60">Cuộn xuống để tải thêm</span>
                  ) : (
                    <span className="text-xs text-muted/60">Đã hiển thị toàn bộ kết quả</span>
                  )}
                </div>
              </div>
            </div>
          )}
        </section>

        {/* Cot phai: Hoc sinh da chon */}
        <section aria-labelledby={`${searchId}-selected`} className="space-y-3">
          <div className="flex items-center justify-between gap-3 h-9">
            <h3 id={`${searchId}-selected`} className="m-0 text-lg font-semibold">
              Học sinh đã chọn
            </h3>
            <span aria-live="polite" className="text-sm font-semibold">
              {selected.size}/{MAX_SELECTED_STUDENTS}
            </span>
          </div>

          {atLimit && (
            <p role="status" className="rounded-lg bg-warning/10 p-2 text-xs text-warning border border-warning/20">
              Đã đạt giới hạn 100 học sinh cho một lần phân công.
            </p>
          )}

          <div className="max-h-[300px] overflow-y-auto border border-line rounded-lg bg-canvas h-[300px]">
            {selected.size === 0 ? (
              <div className="h-full flex items-center justify-center text-muted text-sm p-4">
                Chưa chọn học sinh nào.
              </div>
            ) : (
              <ul className="m-0 divide-y divide-line p-0">
                {[...selected.values()].map((student) => (
                  <li key={student.id} className="flex items-center justify-between gap-3 p-3 hover:bg-canvas/50 transition-colors">
                    <span className="min-w-0">
                      <strong className="block truncate text-sm">{student.displayName || student.fullName}</strong>
                      <span className="text-xs text-muted">{student.studentCode}</span>
                    </span>
                    <Button
                      variant="ghost"
                      className="text-xs min-h-8 h-8 px-2 py-1 text-muted hover:text-danger"
                      aria-label={`Bỏ chọn ${student.displayName || student.fullName}`}
                      onClick={() => updateSelection(student, false)}
                    >
                      Bỏ chọn
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {(validationError || selected.size === 0) && (
            <p role="alert" className="text-xs text-danger mt-1">
              {validationError || "Chọn ít nhất một học sinh trước khi lưu bản nháp."}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}


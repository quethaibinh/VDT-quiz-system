import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useEffect, useId, useState } from "react";
import { Pagination } from "@/components/shared/pagination";
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
  const [page, setPage] = useState(0);

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedKeyword(keyword.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [keyword]);

  const students = useQuery({
    queryKey: ["teacher", "students", debouncedKeyword, page, PAGE_SIZE],
    queryFn: () =>
      searchStudents({
        keyword: debouncedKeyword,
        page,
        size: PAGE_SIZE,
        sort: "studentCode,asc",
      }),
    placeholderData: keepPreviousData,
  });

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
            setPage(0);
          }}
        />
      </div>

      <section aria-labelledby={`${searchId}-results`} className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h3 id={`${searchId}-results`} className="m-0 text-lg">
            Kết quả tìm kiếm
          </h3>
          {students.isFetching && (
            <span role="status" className="text-sm text-muted">
              Đang tìm học sinh...
            </span>
          )}
        </div>

        {students.isError && (
          <div role="alert" className="rounded-lg border border-danger/30 p-4 text-danger">
            <p className="m-0">{getApiErrorMessage(students.error)}</p>
            <Button className="mt-3" variant="secondary" onClick={() => void students.refetch()}>
              Thử lại
            </Button>
          </div>
        )}

        {students.isSuccess && students.data.content.length === 0 && (
          <p role="status" className="rounded-lg bg-canvas p-4 text-muted">
            Không tìm thấy học sinh phù hợp.
          </p>
        )}

        {students.data && students.data.content.length > 0 && (
          <>
            <ul className="m-0 divide-y divide-line rounded-lg border border-line p-0">
              {students.data.content.map((student) => {
                const isSelected = selected.has(student.id);
                const displayName = student.displayName || student.fullName;
                return (
                  <li key={student.id} className="flex items-center gap-3 p-4">
                    <input
                      id={`${searchId}-${student.id}`}
                      type="checkbox"
                      checked={isSelected}
                      disabled={!isSelected && atLimit}
                      onChange={(event) => updateSelection(student, event.target.checked)}
                    />
                    <label htmlFor={`${searchId}-${student.id}`} className="min-w-0 flex-1 cursor-pointer">
                      <strong className="block truncate">{displayName}</strong>
                      <span className="text-sm text-muted">
                        {student.studentCode}
                        {student.fullName !== displayName ? ` · ${student.fullName}` : ""}
                      </span>
                    </label>
                  </li>
                );
              })}
            </ul>
            <Pagination page={students.data.page} totalPages={students.data.totalPages} onChange={setPage} />
          </>
        )}
      </section>

      <section aria-labelledby={`${searchId}-selected`} className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h3 id={`${searchId}-selected`} className="m-0 text-lg">
            Học sinh đã chọn
          </h3>
          <span aria-live="polite" className="text-sm font-semibold">
            {selected.size}/{MAX_SELECTED_STUDENTS}
          </span>
        </div>
        {atLimit && (
          <p role="status" className="rounded-lg bg-warning/10 p-3 text-sm text-warning">
            Đã đạt giới hạn 100 học sinh cho một lần phân công.
          </p>
        )}
        {selected.size === 0 ? (
          <p className="rounded-lg bg-canvas p-4 text-muted">Chưa chọn học sinh nào.</p>
        ) : (
          <ul className="m-0 space-y-2 p-0">
            {[...selected.values()].map((student) => (
              <li key={student.id} className="flex items-center justify-between gap-3 rounded-lg bg-canvas p-3">
                <span className="min-w-0">
                  <strong className="block truncate">{student.displayName || student.fullName}</strong>
                  <span className="text-sm text-muted">{student.studentCode}</span>
                </span>
                <Button
                  variant="ghost"
                  aria-label={`Bỏ chọn ${student.displayName || student.fullName}`}
                  onClick={() => updateSelection(student, false)}
                >
                  Bỏ chọn
                </Button>
              </li>
            ))}
          </ul>
        )}
        {(validationError || selected.size === 0) && (
          <p role="alert" className="text-sm text-danger">
            {validationError || "Chọn ít nhất một học sinh trước khi lưu bản nháp."}
          </p>
        )}
      </section>
    </div>
  );
}

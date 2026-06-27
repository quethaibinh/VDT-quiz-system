import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ArrowRight, CalendarDays, GraduationCap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DataState } from "@/components/shared/data-state";
import { studentExamRepository } from "@/features/student/exams/api/student-exam-repository";
import { StudentExamCard } from "@/features/student/exams/components/student-exam-card";

// Trang dashboard chinh cua hoc sinh voi du lieu ca thi that
export function StudentDashboardPage() {
  const openExams = useQuery({
    queryKey: ["student", "dashboard", "open-exams"],
    queryFn: () => studentExamRepository.listStudentExams({ status: "OPEN", page: 0, size: 3 }),
  });

  const upcomingExams = useQuery({
    queryKey: ["student", "dashboard", "upcoming-exams"],
    queryFn: () => studentExamRepository.listStudentExams({ status: "UPCOMING", page: 0, size: 3 }),
  });

  const loading = openExams.isLoading || upcomingExams.isLoading;
  const error = openExams.error ?? upcomingExams.error;
  const exams = [
    ...(openExams.data?.content ?? []),
    ...(upcomingExams.data?.content ?? []),
  ].slice(0, 4);

  return (
    <div className="space-y-7 p-4 md:p-6">
      <section className="flex flex-col gap-4 rounded-xl border border-line bg-surface p-5 shadow-soft sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <div className="rounded-lg bg-primary/10 p-3 text-primary">
            <GraduationCap size={30} />
          </div>
          <div>
            <h1 className="m-0 text-2xl font-bold text-ink">Không gian học sinh</h1>
            <p className="m-0 mt-1 text-sm text-muted">Theo dõi ca thi được phân công và vào phòng chờ đúng giờ.</p>
          </div>
        </div>
        <Link to="/student/exams">
          <Button variant="primary">
            Ca thi của tôi <ArrowRight size={16} />
          </Button>
        </Link>
      </section>

      <section className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="m-0 text-xl font-bold text-ink">Ca thi cần chú ý</h2>
            <p className="m-0 mt-1 text-sm text-muted">Ưu tiên ca thi đang mở và sắp diễn ra.</p>
          </div>
          <CalendarDays className="hidden text-muted sm:block" size={22} />
        </div>

        <DataState
          loading={loading}
          error={error ? "Không thể tải danh sách ca thi. Vui lòng thử lại." : null}
          empty={exams.length === 0}
          emptyMessage="Bạn chưa có ca thi đang mở hoặc sắp diễn ra."
          onRetry={() => {
            openExams.refetch();
            upcomingExams.refetch();
          }}
        >
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {exams.map((exam) => (
              <StudentExamCard key={exam.examId} exam={exam} />
            ))}
          </div>
        </DataState>
      </section>
    </div>
  );
}

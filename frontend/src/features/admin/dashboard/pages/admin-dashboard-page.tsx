import { useQuery } from "@tanstack/react-query";
import { BookOpen, Upload, Users } from "lucide-react";
import { Link } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { adminSubjectListQuery } from "@/features/admin/subjects/model/admin-subject-queries";
import { adminUserStatisticsQuery } from "@/features/admin/users/model/admin-user-queries";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function AdminDashboardPage() {
  const users = useQuery(adminUserStatisticsQuery());
  const subjects = useQuery(adminSubjectListQuery());
  const activeSubjects = subjects.data?.filter((item) => item.status === "ACTIVE").length ?? 0;
  const archivedSubjects = subjects.data?.filter((item) => item.status === "ARCHIVED").length ?? 0;
  return <div className="space-y-8"><PageHeader title="Tổng quan" description="Các chỉ số dưới đây được tải trực tiếp từ Auth Service và Question Service." /><section><h2 className="text-2xl">Người dùng</h2><DataState loading={users.isLoading} error={users.error ? getApiErrorMessage(users.error) : null} empty={false} onRetry={() => users.refetch()}><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{users.data && <><Metric label="Người dùng đang hoạt động" value={users.data.activeUsers} /><Metric label="Giáo viên đang hoạt động" value={users.data.activeTeachers} /><Metric label="Học sinh đang hoạt động" value={users.data.activeStudents} /></>}</div></DataState></section><section><h2 className="text-2xl">Môn học</h2><DataState loading={subjects.isLoading} error={subjects.error ? getApiErrorMessage(subjects.error) : null} empty={false} onRetry={() => subjects.refetch()}><div className="grid gap-4 sm:grid-cols-2"><Metric label="Môn học đang hoạt động" value={activeSubjects} /><Metric label="Môn học đã lưu trữ" value={archivedSubjects} /></div></DataState></section><section><h2 className="text-2xl">Thao tác nhanh</h2><div className="grid gap-4 md:grid-cols-3"><QuickAction to="/admin/users" icon={Users} title="Quản lý người dùng" description="Tìm kiếm, cập nhật hồ sơ và trạng thái." /><QuickAction to="/admin/users/import" icon={Upload} title="Import tài khoản" description="Tạo tài khoản từ tệp Excel .xlsx." /><QuickAction to="/admin/subjects" icon={BookOpen} title="Quản lý môn học" description="Tạo môn học và phân công giáo viên." /></div></section></div>;
}
function Metric({ label, value }: { label: string; value: number }) { return <article className="rounded-xl border border-line bg-surface p-5 shadow-soft"><p className="m-0 text-sm text-muted">{label}</p><strong className="mt-2 block font-serif text-5xl text-primary">{value}</strong></article>; }
function QuickAction({ to, icon: Icon, title, description }: { to: string; icon: typeof Users; title: string; description: string }) { return <Link to={to} className="rounded-xl border border-line bg-surface p-5 text-ink no-underline shadow-soft transition hover:border-primary"><Icon className="h-7 w-7 text-primary" /><h3 className="mb-1 mt-4 text-xl">{title}</h3><p className="m-0 text-sm text-muted">{description}</p></Link>; }

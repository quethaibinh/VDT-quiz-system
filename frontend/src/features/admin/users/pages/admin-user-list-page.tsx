import { useQuery } from "@tanstack/react-query";
import { Search, Upload } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { StatusChip } from "@/components/ui/status-chip";
import { adminUserListQuery } from "@/features/admin/users/model/admin-user-queries";
import type { AdminUserSummary, ManagedUserRole, UserStatus } from "@/features/admin/users/model/admin-user-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function AdminUserListPage() {
  const [search, setSearch] = useSearchParams();
  const location = useLocation();
  const keyword = search.get("keyword") ?? "";
  const [keywordInput, setKeywordInput] = useState(keyword);
  const role = (search.get("role") || undefined) as ManagedUserRole | undefined;
  const status = (search.get("status") || undefined) as UserStatus | undefined;
  const page = Math.max(0, Number(search.get("page") || 0));
  const sort = search.get("sort") || "fullName,asc";

  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (keywordInput === keyword) return;
      setSearch((current) => {
        const next = new URLSearchParams(current);
        if (keywordInput.trim()) next.set("keyword", keywordInput.trim());
        else next.delete("keyword");
        next.delete("page");
        return next;
      });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keyword, keywordInput, setSearch]);

  const query = useQuery(adminUserListQuery({ keyword: keyword || undefined, userType: role, status, page, size: 20, sort }));
  const update = (name: string, value?: string) => setSearch((current) => {
    const next = new URLSearchParams(current);
    if (value) next.set(name, value);
    else next.delete(name);
    if (name !== "page") next.delete("page");
    return next;
  });

  return (
    <div className="space-y-7">
      <PageHeader title="Người dùng" description="Tra cứu và quản lý hồ sơ giáo viên, học sinh từ dữ liệu xác thực." action={<Link to="/admin/users/import"><Button><Upload className="h-4 w-4" />Import tài khoản</Button></Link>} />
      <FilterBar>
        <label className="relative min-w-56 flex-1"><span className="sr-only">Tìm người dùng</span><Search className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input value={keywordInput} onChange={(event) => setKeywordInput(event.target.value)} className="pl-10" placeholder="Tên, mã, email hoặc tài khoản..." /></label>
        <Select aria-label="Vai trò" value={role ?? ""} onChange={(event) => update("role", event.target.value)}><option value="">Mọi vai trò</option><option value="TEACHER">Giáo viên</option><option value="STUDENT">Học sinh</option></Select>
        <Select aria-label="Trạng thái" value={status ?? ""} onChange={(event) => update("status", event.target.value)}><option value="">Mọi trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option></Select>
        <Select aria-label="Sắp xếp" value={sort} onChange={(event) => update("sort", event.target.value)}><option value="fullName,asc">Tên A–Z</option><option value="fullName,desc">Tên Z–A</option><option value="createdAt,desc">Mới nhất</option><option value="status,asc">Trạng thái</option></Select>
      </FilterBar>
      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.content.length} emptyMessage="Không tìm thấy người dùng phù hợp." onRetry={() => query.refetch()}>
        <div className="hidden overflow-hidden rounded-xl border border-line bg-surface shadow-soft md:block">
          <table className="w-full border-collapse text-left text-sm"><thead className="bg-primary/5 text-muted"><tr><th className="p-4">Người dùng</th><th className="p-4">Mã</th><th className="p-4">Vai trò</th><th className="p-4">Trạng thái</th><th className="p-4"></th></tr></thead><tbody>{query.data?.content.map((user) => <UserRow key={user.id} user={user} returnTo={`${location.pathname}${location.search}`} />)}</tbody></table>
        </div>
        <div className="grid gap-3 md:hidden">{query.data?.content.map((user) => <UserCard key={user.id} user={user} returnTo={`${location.pathname}${location.search}`} />)}</div>
      </DataState>
      {query.data && <Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={(next) => update("page", String(next))} />}
    </div>
  );
}

const codeOf = (user: AdminUserSummary) => user.teacherCode || user.studentCode || "—";
const roleOf = (user: AdminUserSummary) => user.userType === "TEACHER" ? "Giáo viên" : "Học sinh";

function UserRow({ user, returnTo }: { user: AdminUserSummary; returnTo: string }) {
  return <tr className="border-t border-line"><td className="p-4"><strong>{user.fullName}</strong><p className="m-0 text-xs text-muted">{user.email || user.username}</p></td><td className="p-4">{codeOf(user)}</td><td className="p-4">{roleOf(user)}</td><td className="p-4"><StatusChip tone={user.status === "ACTIVE" ? "success" : "neutral"}>{user.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}</StatusChip></td><td className="p-4 text-right"><Link className="font-semibold text-primary" to={`/admin/users/${user.id}?returnTo=${encodeURIComponent(returnTo)}`}>Xem chi tiết</Link></td></tr>;
}
function UserCard({ user, returnTo }: { user: AdminUserSummary; returnTo: string }) {
  return <article className="rounded-xl border border-line bg-surface p-4 shadow-soft"><div className="flex items-start justify-between gap-3"><div><h2 className="m-0 text-xl">{user.fullName}</h2><p className="mt-1 text-sm text-muted">{codeOf(user)} · {roleOf(user)}</p></div><StatusChip tone={user.status === "ACTIVE" ? "success" : "neutral"}>{user.status === "ACTIVE" ? "Hoạt động" : "Ngừng"}</StatusChip></div><Link className="mt-4 inline-block font-semibold text-primary" to={`/admin/users/${user.id}?returnTo=${encodeURIComponent(returnTo)}`}>Xem chi tiết</Link></article>;
}

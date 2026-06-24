import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Search } from "lucide-react";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { StatusChip } from "@/components/ui/status-chip";
import { SubjectFormDialog } from "@/features/admin/subjects/components/subject-form-dialog";
import { createAdminSubject, updateAdminSubject } from "@/features/admin/subjects/api/admin-subject-api";
import { adminSubjectKeys, adminSubjectListQuery } from "@/features/admin/subjects/model/admin-subject-queries";
import type { AdminSubject, AdminSubjectInput, SubjectStatus } from "@/features/admin/subjects/model/admin-subject-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function AdminSubjectListPage() {
  const [search, setSearch] = useSearchParams();
  const keyword = search.get("keyword") ?? "";
  const status = (search.get("status") || undefined) as SubjectStatus | undefined;
  const [editing, setEditing] = useState<AdminSubject | null | undefined>(undefined);
  const queryClient = useQueryClient();
  const query = useQuery(adminSubjectListQuery(keyword, status));
  const mutation = useMutation({
    mutationFn: ({ subject, input }: { subject?: AdminSubject | null; input: AdminSubjectInput }) => subject ? updateAdminSubject(subject.id, input) : createAdminSubject(input),
    onSuccess: async () => { setEditing(undefined); await queryClient.invalidateQueries({ queryKey: adminSubjectKeys.all }); },
  });
  const update = (name: string, value: string) => setSearch((current) => {
    const next = new URLSearchParams(current);
    if (value) next.set(name, value);
    else next.delete(name);
    return next;
  });
  return <div className="space-y-7"><PageHeader title="Môn học" description="Quản lý vòng đời môn học và phân công giáo viên bằng dữ liệu thật." action={<Button onClick={() => setEditing(null)}><Plus className="h-4 w-4" />Tạo môn học</Button>} /><FilterBar><label className="relative min-w-56 flex-1"><span className="sr-only">Tìm môn học</span><Search className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input value={keyword} onChange={(event) => update("keyword", event.target.value)} className="pl-10" placeholder="Tìm theo mã hoặc tên..." /></label><Select aria-label="Trạng thái môn học" value={status ?? ""} onChange={(event) => update("status", event.target.value)}><option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="ARCHIVED">Đã lưu trữ</option></Select></FilterBar><DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.length} emptyMessage="Chưa có môn học phù hợp." onRetry={() => query.refetch()}><div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{query.data?.map((subject) => <article key={subject.id} className="rounded-xl border border-line bg-surface p-5 shadow-soft"><div className="flex items-start justify-between gap-3"><span className="rounded-lg bg-primary/10 px-3 py-1 font-mono text-sm font-bold text-primary">{subject.code}</span><StatusChip tone={subject.status === "ACTIVE" ? "success" : "neutral"}>{subject.status === "ACTIVE" ? "Hoạt động" : "Lưu trữ"}</StatusChip></div><h2 className="mt-5 text-2xl">{subject.name}</h2><p className="min-h-10 text-sm text-muted">{subject.description || "Chưa có mô tả."}</p><div className="mt-5 flex items-center justify-between"><Link className="font-semibold text-primary" to={`/admin/subjects/${subject.id}`}>Chi tiết & phân công</Link><Button variant="ghost" aria-label={`Sửa ${subject.name}`} onClick={() => setEditing(subject)}><Pencil className="h-4 w-4" /></Button></div></article>)}</div></DataState><SubjectFormDialog open={editing !== undefined} subject={editing} loading={mutation.isPending} error={mutation.error ? getApiErrorMessage(mutation.error) : undefined} onOpenChange={(open) => !open && setEditing(undefined)} onSubmit={(input) => mutation.mutate({ subject: editing, input })} /></div>;
}

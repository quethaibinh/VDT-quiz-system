import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Pencil } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { TeacherAssignmentPanel } from "@/features/admin/assignments/components/teacher-assignment-panel";
import { SubjectFormDialog } from "@/features/admin/subjects/components/subject-form-dialog";
import { archiveAdminSubject, restoreAdminSubject, updateAdminSubject } from "@/features/admin/subjects/api/admin-subject-api";
import { adminSubjectDetailQuery, adminSubjectKeys } from "@/features/admin/subjects/model/admin-subject-queries";
import type { AdminSubjectInput } from "@/features/admin/subjects/model/admin-subject-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function AdminSubjectDetailPage() {
  const { subjectId = "" } = useParams();
  const queryClient = useQueryClient();
  const query = useQuery(adminSubjectDetailQuery(subjectId));
  const [editing, setEditing] = useState(false);
  const [confirmLifecycle, setConfirmLifecycle] = useState(false);
  const refresh = async () => { await queryClient.invalidateQueries({ queryKey: adminSubjectKeys.all }); };
  const update = useMutation({ mutationFn: (input: AdminSubjectInput) => updateAdminSubject(subjectId, input), onSuccess: async () => { setEditing(false); await refresh(); } });
  const lifecycle = useMutation({ mutationFn: () => query.data?.status === "ACTIVE" ? archiveAdminSubject(subjectId) : restoreAdminSubject(subjectId), onSuccess: async () => { setConfirmLifecycle(false); await refresh(); } });
  return <div className="space-y-7"><Link to="/admin/subjects" className="inline-flex items-center gap-2 text-sm font-semibold text-primary"><ArrowLeft className="h-4 w-4" />Quay lại danh sách</Link><DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data} onRetry={() => query.refetch()}>{query.data && <><PageHeader title={query.data.name} description={`${query.data.code} · Quản lý thông tin và giáo viên phụ trách`} action={<div className="flex flex-wrap gap-2"><Button variant="secondary" onClick={() => setEditing(true)}><Pencil className="h-4 w-4" />Chỉnh sửa</Button><Button variant={query.data.status === "ACTIVE" ? "danger" : "primary"} onClick={() => setConfirmLifecycle(true)}>{query.data.status === "ACTIVE" ? "Lưu trữ" : "Khôi phục"}</Button></div>} /><section className="rounded-xl border border-line bg-surface p-5 shadow-soft"><div className="flex items-center gap-3"><StatusChip tone={query.data.status === "ACTIVE" ? "success" : "neutral"}>{query.data.status === "ACTIVE" ? "Đang hoạt động" : "Đã lưu trữ"}</StatusChip><span className="font-mono text-sm font-bold text-primary">{query.data.code}</span></div><p className="mb-0 mt-4 text-muted">{query.data.description || "Môn học chưa có mô tả."}</p></section><TeacherAssignmentPanel subjectId={subjectId} archived={query.data.status === "ARCHIVED"} /><SubjectFormDialog open={editing} subject={query.data} loading={update.isPending} error={update.error ? getApiErrorMessage(update.error) : undefined} onOpenChange={setEditing} onSubmit={(input) => update.mutate(input)} /><ConfirmDialog open={confirmLifecycle} onOpenChange={setConfirmLifecycle} title={query.data.status === "ACTIVE" ? "Lưu trữ môn học?" : "Khôi phục môn học?"} description={query.data.status === "ACTIVE" ? "Không thể phân công giáo viên mới khi môn học đã lưu trữ." : "Môn học sẽ hoạt động trở lại và có thể nhận phân công mới."} confirmLabel={query.data.status === "ACTIVE" ? "Lưu trữ" : "Khôi phục"} tone={query.data.status === "ACTIVE" ? "danger" : "primary"} loading={lifecycle.isPending} onConfirm={() => lifecycle.mutate()} />{lifecycle.error && <p role="alert" className="text-sm text-danger">{getApiErrorMessage(lifecycle.error)}</p>}</>}</DataState></div>;
}

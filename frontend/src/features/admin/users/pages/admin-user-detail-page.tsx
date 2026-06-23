import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { StatusChip } from "@/components/ui/status-chip";
import { updateAdminUser, updateAdminUserStatus } from "@/features/admin/users/api/admin-user-api";
import { adminUserDetailQuery, adminUserKeys } from "@/features/admin/users/model/admin-user-queries";
import { getApiErrorMessage } from "@/lib/http/api-error";

const schema = z.object({
  fullName: z.string().trim().min(1, "Họ tên là bắt buộc."),
  displayName: z.string(),
  email: z.union([z.literal(""), z.email("Email không hợp lệ.")]),
  birthDate: z.string(),
  gender: z.enum(["", "MALE", "FEMALE", "OTHER"]),
});
type FormValues = z.infer<typeof schema>;

export function AdminUserDetailPage() {
  const { userId = "" } = useParams();
  const [search] = useSearchParams();
  const returnTo = search.get("returnTo");
  const listPath = returnTo?.startsWith("/admin/users") ? returnTo : "/admin/users";
  const queryClient = useQueryClient();
  const query = useQuery(adminUserDetailQuery(userId));
  const [confirmStatus, setConfirmStatus] = useState(false);
  const [feedback, setFeedback] = useState("");
  const form = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { fullName: "", displayName: "", email: "", birthDate: "", gender: "" } });

  useEffect(() => {
    if (query.data) form.reset({ fullName: query.data.fullName, displayName: query.data.displayName ?? "", email: query.data.email ?? "", birthDate: query.data.birthDate ?? "", gender: query.data.gender ?? "" });
  }, [form, query.data]);

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: adminUserKeys.detail(userId) });
    await queryClient.invalidateQueries({ queryKey: adminUserKeys.lists() });
    await queryClient.invalidateQueries({ queryKey: adminUserKeys.statistics() });
  };
  const save = useMutation({
    mutationFn: (values: FormValues) => updateAdminUser(userId, { fullName: values.fullName, displayName: values.displayName || null, email: values.email || null, birthDate: values.birthDate || null, gender: values.gender || null }),
    onSuccess: async () => { setFeedback("Đã cập nhật hồ sơ."); await refresh(); },
  });
  const status = useMutation({
    mutationFn: () => updateAdminUserStatus(userId, query.data?.status === "ACTIVE" ? "INACTIVE" : "ACTIVE"),
    onSuccess: async () => { setConfirmStatus(false); setFeedback("Đã cập nhật trạng thái tài khoản."); await refresh(); },
  });
  const error = save.error || status.error;

  return (
    <div className="space-y-7">
      <Link to={listPath} className="inline-flex items-center gap-2 text-sm font-semibold text-primary"><ArrowLeft className="h-4 w-4" />Quay lại danh sách</Link>
      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data} onRetry={() => query.refetch()}>
        {query.data && <>
          <PageHeader title={query.data.fullName} description={`${query.data.userType === "TEACHER" ? "Giáo viên" : "Học sinh"} · ${query.data.teacherCode || query.data.studentCode || query.data.username}`} action={<StatusChip tone={query.data.status === "ACTIVE" ? "success" : "neutral"}>{query.data.status === "ACTIVE" ? "Đang hoạt động" : "Ngừng hoạt động"}</StatusChip>} />
          <section className="grid gap-6 lg:grid-cols-[0.8fr_1.2fr]">
            <div className="rounded-xl border border-line bg-surface p-5 shadow-soft"><h2 className="m-0 text-2xl">Thông tin định danh</h2><dl className="mt-5 grid gap-4 text-sm"><div><dt className="text-muted">Tên đăng nhập</dt><dd className="m-0 font-semibold">{query.data.username}</dd></div><div><dt className="text-muted">Mã tài khoản</dt><dd className="m-0 font-semibold">{query.data.teacherCode || query.data.studentCode}</dd></div><div><dt className="text-muted">Vai trò</dt><dd className="m-0 font-semibold">{query.data.userType}</dd></div></dl><Button variant={query.data.status === "ACTIVE" ? "danger" : "secondary"} className="mt-6 w-full" onClick={() => setConfirmStatus(true)}>{query.data.status === "ACTIVE" ? "Ngừng hoạt động" : "Kích hoạt tài khoản"}</Button></div>
            <form onSubmit={form.handleSubmit((values) => save.mutate(values))} className="rounded-xl border border-line bg-surface p-5 shadow-soft"><h2 className="m-0 text-2xl">Hồ sơ an toàn</h2><div className="mt-5 grid gap-4 sm:grid-cols-2"><Field label="Họ và tên" error={form.formState.errors.fullName?.message}><Input {...form.register("fullName")} /></Field><Field label="Tên hiển thị"><Input {...form.register("displayName")} /></Field><Field label="Email" error={form.formState.errors.email?.message}><Input type="email" {...form.register("email")} /></Field><Field label="Ngày sinh"><Input type="date" {...form.register("birthDate")} /></Field><Field label="Giới tính"><Select className="w-full" {...form.register("gender")}><option value="">Chưa cập nhật</option><option value="MALE">Nam</option><option value="FEMALE">Nữ</option><option value="OTHER">Khác</option></Select></Field></div>{feedback && <p role="status" className="mt-4 text-sm text-success">{feedback}</p>}{error && <p role="alert" className="mt-4 text-sm text-danger">{getApiErrorMessage(error)}</p>}<Button type="submit" loading={save.isPending} className="mt-5">Lưu hồ sơ</Button></form>
          </section>
          <ConfirmDialog open={confirmStatus} onOpenChange={setConfirmStatus} title={query.data.status === "ACTIVE" ? "Ngừng hoạt động tài khoản?" : "Kích hoạt tài khoản?"} description={<>Xác nhận thay đổi trạng thái của <strong>{query.data.fullName}</strong> ({query.data.teacherCode || query.data.studentCode || query.data.username}).</>} confirmLabel="Xác nhận" tone={query.data.status === "ACTIVE" ? "danger" : "primary"} loading={status.isPending} onConfirm={() => status.mutate()} />
        </>}
      </DataState>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return <label className="block text-sm font-semibold">{label}<div className="mt-2">{children}</div>{error && <span className="mt-1 block text-xs text-danger">{error}</span>}</label>;
}

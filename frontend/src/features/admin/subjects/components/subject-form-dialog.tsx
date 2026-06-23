import * as Dialog from "@radix-ui/react-dialog";
import { zodResolver } from "@hookform/resolvers/zod";
import { X } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { AdminSubject, AdminSubjectInput } from "@/features/admin/subjects/model/admin-subject-contracts";

const schema = z.object({ code: z.string().trim().min(1, "Mã môn là bắt buộc."), name: z.string().trim().min(1, "Tên môn là bắt buộc."), description: z.string() });
type Values = z.infer<typeof schema>;

export function SubjectFormDialog({ open, subject, loading, error, onOpenChange, onSubmit }: { open: boolean; subject?: AdminSubject | null; loading?: boolean; error?: string; onOpenChange: (open: boolean) => void; onSubmit: (input: AdminSubjectInput) => void }) {
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { code: "", name: "", description: "" } });
  useEffect(() => { if (open) form.reset({ code: subject?.code ?? "", name: subject?.name ?? "", description: subject?.description ?? "" }); }, [form, open, subject]);
  return <Dialog.Root open={open} onOpenChange={(next) => !loading && onOpenChange(next)}><Dialog.Portal><Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35" /><Dialog.Content className="fixed right-0 top-0 z-50 h-full w-[min(100%,520px)] overflow-y-auto border-l border-line bg-surface p-6 shadow-soft"><Dialog.Title className="m-0 text-3xl">{subject ? "Chỉnh sửa môn học" : "Tạo môn học"}</Dialog.Title><Dialog.Description className="mt-2 text-sm text-muted">Thông tin này được lưu trực tiếp vào Question Service.</Dialog.Description><Dialog.Close className="absolute right-4 top-4 rounded-lg p-2" aria-label="Đóng"><X /></Dialog.Close><form className="mt-7 space-y-5" onSubmit={form.handleSubmit((values) => onSubmit({ ...values, description: values.description.trim() || null }))}><Field label="Mã môn" error={form.formState.errors.code?.message}><Input {...form.register("code")} /></Field><Field label="Tên môn" error={form.formState.errors.name?.message}><Input {...form.register("name")} /></Field><label className="block text-sm font-semibold">Mô tả<textarea className="mt-2 min-h-32 w-full rounded-lg border border-line bg-surface p-3 text-sm" {...form.register("description")} /></label>{error && <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">{error}</p>}<div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => onOpenChange(false)}>Hủy</Button><Button type="submit" loading={loading}>Lưu môn học</Button></div></form></Dialog.Content></Dialog.Portal></Dialog.Root>;
}
function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) { return <label className="block text-sm font-semibold">{label}<div className="mt-2">{children}</div>{error && <span className="mt-1 block text-xs text-danger">{error}</span>}</label>; }

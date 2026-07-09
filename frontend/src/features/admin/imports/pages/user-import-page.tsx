import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FileSpreadsheet, Upload } from "lucide-react";
import { useState } from "react";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { importAdminUsers } from "@/features/admin/imports/api/admin-user-import-api";
import { adminUserKeys } from "@/features/admin/users/model/admin-user-queries";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function UserImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: importAdminUsers,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminUserKeys.all });
    },
  });
  return <div className="space-y-7"><PageHeader title="Import tài khoản" description="Tạo tài khoản giáo viên và học sinh từ tệp Excel .xlsx. Máy chủ sẽ kiểm tra toàn bộ dữ liệu." /><section className="rounded-2xl border border-line bg-surface p-6 shadow-soft"><label className="grid min-h-48 cursor-pointer place-items-center rounded-xl border-2 border-dashed border-line bg-canvas p-6 text-center hover:border-primary"><input className="sr-only" type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => { setFile(event.target.files?.[0] ?? null); mutation.reset(); }} /><span><FileSpreadsheet className="mx-auto h-10 w-10 text-primary" /><strong className="mt-3 block">{file ? file.name : "Chọn tệp Excel"}</strong><span className="mt-1 block text-sm text-muted">{file ? `${(file.size / 1024).toFixed(1)} KB` : "Chỉ chấp nhận định dạng .xlsx"}</span></span></label><Button className="mt-5" disabled={!file} loading={mutation.isPending} onClick={() => file && mutation.mutate(file)}><Upload className="h-4 w-4" />Bắt đầu import</Button>{mutation.error && <p role="alert" className="mt-4 rounded-lg bg-danger/10 p-3 text-sm text-danger">{getApiErrorMessage(mutation.error)}</p>}</section>{mutation.data && <section className="space-y-4 rounded-2xl border border-line bg-surface p-6 shadow-soft"><h2 className="m-0 text-3xl">Kết quả import</h2><div className="grid gap-3 sm:grid-cols-3"><Metric label="Tổng dòng" value={mutation.data.totalRows} /><Metric label="Thành công" value={mutation.data.successCount} tone="text-success" /><Metric label="Lỗi" value={mutation.data.failedCount} tone="text-danger" /></div>{mutation.data.errors.length > 0 && <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b border-line"><th className="p-3">Dòng</th><th className="p-3">Trường</th><th className="p-3">Mã lỗi</th><th className="p-3">Chi tiết</th></tr></thead><tbody>{mutation.data.errors.map((error, index) => <tr key={`${error.rowNumber}-${error.fieldName}-${index}`} className="border-b border-line/70"><td className="p-3">{error.rowNumber}</td><td className="p-3">{error.fieldName}</td><td className="p-3 font-mono text-xs">{error.errorCode}</td><td className="p-3">{error.message}</td></tr>)}</tbody></table></div>}</section>}</div>;
}
function Metric({ label, value, tone = "" }: { label: string; value: number; tone?: string }) { return <div className="rounded-xl bg-canvas p-4"><span className="text-sm text-muted">{label}</span><strong className={`mt-1 block text-3xl ${tone}`}>{value}</strong></div>; }

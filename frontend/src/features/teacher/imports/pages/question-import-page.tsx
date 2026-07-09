import { useMutation, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, FileSpreadsheet, Upload, XCircle } from "lucide-react";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { importQuestions } from "@/features/teacher/imports/api/question-import-api";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function QuestionImportPage() {
  const { subjectId = "" } = useParams();
  const [file, setFile] = useState<File | null>(null);
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error("Vui lòng chọn file .xlsx.");
      return importQuestions(subjectId, file);
    },
    onSuccess: (result) => {
      if (result.imported) queryClient.invalidateQueries({ queryKey: ["teacher", "questions", subjectId] });
    },
  });

  return <div className="space-y-6">
    <PageHeader title="Import câu hỏi" description="Hệ thống kiểm tra toàn bộ file. Nếu có dòng lỗi, không câu hỏi nào được lưu." action={<Link to=".."><Button variant="secondary">Về ngân hàng</Button></Link>} />
    <section className="rounded-xl border border-dashed border-primary/40 bg-surface p-8 text-center shadow-soft">
      <FileSpreadsheet className="mx-auto h-12 w-12 text-primary" />
      <p className="font-semibold">{file?.name ?? "Chọn file Excel .xlsx"}</p>
      <input id="question-file" className="sr-only" type="file" accept=".xlsx" onChange={(e) => { setFile(e.target.files?.[0] ?? null); mutation.reset(); }} />
      <label htmlFor="question-file" className="inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border border-line px-4 py-2 text-sm font-semibold"><Upload className="h-4 w-4" />Chọn file</label>
      <Button className="ml-3" disabled={!file} loading={mutation.isPending} onClick={() => mutation.mutate()}>Bắt đầu import</Button>
    </section>
    {mutation.error && <p role="alert" className="rounded-lg bg-danger/10 p-4 text-danger">{getApiErrorMessage(mutation.error)}</p>}
    {mutation.data && <section className="rounded-xl border border-line bg-surface p-6 shadow-soft">
      <div className="flex items-center gap-3">{mutation.data.imported ? <CheckCircle2 className="text-success" /> : <XCircle className="text-danger" />}<h2 className="m-0 text-2xl">{mutation.data.imported ? "Import thành công" : "File cần được sửa"}</h2></div>
      <div className="mt-5 grid gap-3 sm:grid-cols-3"><div><strong>{mutation.data.totalRows}</strong><small className="block text-muted">Tổng số dòng</small></div><div><strong>{mutation.data.createdQuestionCount}</strong><small className="block text-muted">Câu hỏi đã tạo</small></div><div><strong>{mutation.data.createdTopicCount}</strong><small className="block text-muted">Chủ đề mới</small></div></div>
      {mutation.data.errors.length > 0 && <div className="mt-6 overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b border-line"><th className="p-3">Dòng</th><th>Trường</th><th>Mã lỗi</th><th>Mô tả</th></tr></thead><tbody>{mutation.data.errors.map((error) => <tr key={`${error.rowNumber}-${error.fieldName}`} className="border-b border-line"><td className="p-3">{error.rowNumber}</td><td>{error.fieldName}</td><td className="font-mono text-xs text-danger">{error.errorCode}</td><td>{error.message}</td></tr>)}</tbody></table></div>}
    </section>}
  </div>;
}

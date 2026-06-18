import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { listCollections } from "@/features/teacher/collections";
import { createExam } from "@/features/teacher/exams/api/exam-repository";
import type { ExamDraftInput } from "@/features/teacher/exams/model/exam-contracts";
import { subjectDetailQuery } from "@/features/teacher/subjects";
import { getApiErrorMessage } from "@/lib/http/api-error";

const makeInitial = (subjectId: string): ExamDraftInput => ({
  title: "", subjectId, collectionId: "", startAt: "", durationMinutes: 60,
  easyCount: 20, mediumCount: 20, hardCount: 10,
  shuffleQuestions: true, shuffleOptions: true,
});

export function ExamBuilderPage() {
  const { subjectId = "" } = useParams();
  const [step, setStep] = useState(1);
  const [draft, setDraft] = useState(() => makeInitial(subjectId));
  const navigate = useNavigate();
  const subject = useQuery(subjectDetailQuery(subjectId));
  const collections = useQuery({
    queryKey: ["teacher", "collections", subjectId, "exam-builder"],
    queryFn: () => listCollections(subjectId, { ownership: "ALL", status: "ACTIVE", page: 0, size: 100, sort: "updatedAt,desc" }),
    enabled: Boolean(subjectId) && subject.isSuccess,
  });
  const create = useMutation({
    mutationFn: () => createExam(draft),
    onSuccess: () => navigate(`/teacher/subjects/${subjectId}/exams`),
  });
  const total = draft.easyCount + draft.mediumCount + draft.hardCount;
  const selectedCollection = useMemo(() => collections.data?.content.find((item) => item.id === draft.collectionId), [collections.data, draft.collectionId]);
  const valid = draft.title && draft.collectionId && draft.startAt && draft.durationMinutes > 0 && total > 0;
  const set = <K extends keyof ExamDraftInput>(key: K, value: ExamDraftInput[K]) => setDraft((current) => ({ ...current, [key]: value }));

  return <div className="space-y-6">
    <PageHeader title="Tạo ca thi" description={subject.data ? `${subject.data.name} · ${subject.data.code}` : "Đang tải thông tin môn học..."} />
    <div className="grid grid-cols-5 gap-2">{["Thông tin", "Câu hỏi", "Học sinh", "Cấu hình", "Xem lại"].map((label, index) => <button key={label} className={`rounded-lg px-2 py-3 text-xs font-semibold md:text-sm ${step === index + 1 ? "bg-primary text-white" : "bg-surface text-muted"}`} onClick={() => setStep(index + 1)}>{index + 1}. {label}</button>)}</div>
    <div className="grid gap-6 xl:grid-cols-[1fr_340px]">
      <section className="rounded-xl border border-line bg-surface p-6 shadow-soft">
        {step === 1 && <div className="space-y-4"><label className="block text-sm font-semibold">Tên ca thi<Input className="mt-2" value={draft.title} onChange={(e) => set("title", e.target.value)} /></label><div className="rounded-lg bg-canvas p-4"><span className="text-sm text-muted">Môn học</span><strong className="mt-1 block">{subject.data?.name || "Đang tải..."}</strong></div><label className="block text-sm font-semibold">Bắt đầu<Input className="mt-2" type="datetime-local" value={draft.startAt} onChange={(e) => set("startAt", e.target.value)} /></label><label className="block text-sm font-semibold">Thời lượng (phút)<Input className="mt-2" type="number" min={1} value={draft.durationMinutes} onChange={(e) => set("durationMinutes", Number(e.target.value))} /></label></div>}
        {step === 2 && <div className="space-y-5"><label className="block text-sm font-semibold">Bộ câu hỏi<Select className="mt-2 w-full" value={draft.collectionId} onChange={(e) => set("collectionId", e.target.value)}><option value="">Chọn bộ câu hỏi</option>{collections.data?.content.map((item) => <option key={item.id} value={item.id}>{item.name} ({item.stats.questionCount} câu)</option>)}</Select></label>{(["easyCount", "mediumCount", "hardCount"] as const).map((key, index) => <label key={key} className="grid grid-cols-[1fr_120px_auto] items-center gap-3"><span>{["Dễ", "Vừa", "Khó"][index]}</span><Input type="number" min={0} value={draft[key]} onChange={(e) => set(key, Number(e.target.value))} /><span className="text-sm text-muted">/ {selectedCollection ? [selectedCollection.stats.easy, selectedCollection.stats.medium, selectedCollection.stats.hard][index] : 0}</span></label>)}</div>}
        {step === 3 && <div><h2>Phân công học sinh</h2><p className="text-muted">Chưa có API tìm học sinh. Bước này dùng dữ liệu mô phỏng và sẽ được nối sau.</p><div className="rounded-lg bg-canvas p-4">120 học sinh mẫu được chọn cho bản demo.</div></div>}
        {step === 4 && <div className="space-y-4"><label className="flex items-center justify-between rounded-lg border border-line p-4"><span><strong>Trộn thứ tự câu hỏi</strong><small className="block text-muted">Mỗi học sinh có thứ tự khác nhau</small></span><input type="checkbox" checked={draft.shuffleQuestions} onChange={(e) => set("shuffleQuestions", e.target.checked)} /></label><label className="flex items-center justify-between rounded-lg border border-line p-4"><span><strong>Trộn thứ tự đáp án</strong><small className="block text-muted">Trộn phương án trong mỗi câu</small></span><input type="checkbox" checked={draft.shuffleOptions} onChange={(e) => set("shuffleOptions", e.target.checked)} /></label></div>}
        {step === 5 && <div><h2>Xem lại cấu hình</h2><dl className="grid gap-3 sm:grid-cols-2"><div><dt className="text-sm text-muted">Tên ca thi</dt><dd className="m-0 font-semibold">{draft.title || "Chưa nhập"}</dd></div><div><dt className="text-sm text-muted">Tổng câu</dt><dd className="m-0 font-semibold">{total}</dd></div><div><dt className="text-sm text-muted">Bắt đầu</dt><dd className="m-0 font-semibold">{draft.startAt || "Chưa chọn"}</dd></div><div><dt className="text-sm text-muted">Thời lượng</dt><dd className="m-0 font-semibold">{draft.durationMinutes} phút</dd></div></dl></div>}
        <div className="mt-8 flex justify-between"><Button variant="secondary" disabled={step === 1} onClick={() => setStep((value) => value - 1)}>Quay lại</Button>{step < 5 ? <Button onClick={() => setStep((value) => value + 1)}>Tiếp tục</Button> : <Button disabled={!valid} loading={create.isPending} onClick={() => create.mutate()}>Lưu bản nháp</Button>}</div>
        {(subject.error || collections.error || create.error) && <p className="text-danger">{getApiErrorMessage(subject.error ?? collections.error ?? create.error)}</p>}
      </section>
      <aside className="h-fit rounded-xl border border-line bg-surface p-6 shadow-soft"><h2 className="mt-0">Tóm tắt ca thi</h2><p className="font-semibold">{draft.title || "Ca thi chưa đặt tên"}</p><p className="text-sm text-muted">{total} câu · {draft.durationMinutes} phút</p><div className="mt-5 flex h-3 overflow-hidden rounded-full bg-line"><span className="bg-success" style={{ width: `${total ? draft.easyCount / total * 100 : 0}%` }} /><span className="bg-warning" style={{ width: `${total ? draft.mediumCount / total * 100 : 0}%` }} /><span className="bg-danger" style={{ width: `${total ? draft.hardCount / total * 100 : 0}%` }} /></div></aside>
    </div>
  </div>;
}

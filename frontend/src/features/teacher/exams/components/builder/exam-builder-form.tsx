import { useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { listCollections } from "@/features/teacher/collections";
import {
  createInitialBuilderState,
  type ExamBuilderState,
  validateBuilderState,
  validateBuilderStep,
} from "@/features/teacher/exams/components/builder/builder-state";
import { StudentAssignmentStep } from "@/features/teacher/exams/components/builder/student-assignment-step";

interface ExamBuilderFormProps {
  subjectId: string;
  initialState?: ExamBuilderState;
  submitLabel: string;
  submitting?: boolean;
  error?: string | null;
  notice?: string | null;
  onSubmit: (state: ExamBuilderState) => void;
}

const steps = ["Thông tin", "Câu hỏi", "Học sinh", "Chính sách", "Xem lại"];
type ExamBuilderStep = 1 | 2 | 3 | 4 | 5;
const questionCounts = [
  { key: "easyCount", label: "Dễ", capacityKey: "easy" },
  { key: "mediumCount", label: "Vừa", capacityKey: "medium" },
  { key: "hardCount", label: "Khó", capacityKey: "hard" },
] as const;

export function ExamBuilderForm({
  subjectId,
  initialState,
  submitLabel,
  submitting,
  error,
  notice,
  onSubmit,
}: ExamBuilderFormProps) {
  const [step, setStep] = useState(1);
  const [state, setState] = useState<ExamBuilderState>(
    () => initialState ?? createInitialBuilderState(),
  );
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const collections = useQuery({
    queryKey: ["teacher", "collections", subjectId, "exam-builder"],
    queryFn: () =>
      listCollections(subjectId, {
        ownership: "ALL",
        status: "ACTIVE",
        page: 0,
        size: 100,
        sort: "updatedAt,desc",
      }),
    enabled: Boolean(subjectId),
  });
  const selectedCollection = useMemo(
    () => collections.data?.content.find((item) => item.id === state.collectionId),
    [collections.data, state.collectionId],
  );
  const capacity = selectedCollection?.stats;
  const total = state.easyCount + state.mediumCount + state.hardCount;

  const set = <K extends keyof ExamBuilderState>(key: K, value: ExamBuilderState[K]) => {
    setState((current) => ({ ...current, [key]: value }));
    setValidationErrors([]);
  };

  const validateStep = (currentStep: ExamBuilderStep) => {
    const errors = validateBuilderStep(state, currentStep, capacity);
    setValidationErrors(errors);
    return errors.length === 0;
  };

  const submit = () => {
    const errors = validateBuilderState(state, capacity);
    setValidationErrors(errors);
    if (errors.length === 0) onSubmit(state);
  };

  return (
    <div className="space-y-6">
      <nav className="flex gap-2 overflow-x-auto pb-1" aria-label="Các bước tạo ca thi">
        {steps.map((label, index) => {
          const number = index + 1;
          return (
            <button
              key={label}
              type="button"
              aria-current={step === number ? "step" : undefined}
              className={`min-w-32 flex-1 rounded-lg px-3 py-3 text-sm font-semibold ${
                step === number ? "bg-primary text-white" : "bg-surface text-muted"
              }`}
              onClick={() => {
                if (number < step || validateStep(step as ExamBuilderStep)) setStep(number);
              }}
            >
              {number}. {label}
            </button>
          );
        })}
      </nav>

      {notice && <p role="status" className="rounded-lg bg-warning/10 p-4 text-sm text-warning">{notice}</p>}
      {error && <p role="alert" className="rounded-lg bg-danger/10 p-4 text-sm text-danger">{error}</p>}
      {validationErrors.length > 0 && (
        <div role="alert" className="rounded-lg border border-danger/30 bg-danger/5 p-4 text-sm text-danger">
          <p className="mt-0 font-semibold">Vui lòng kiểm tra các nội dung sau:</p>
          <ul className="mb-0">
            {validationErrors.map((message) => <li key={message}>{message}</li>)}
          </ul>
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <section className="rounded-xl border border-line bg-surface p-6 shadow-soft">
          {step === 1 && (
            <div className="space-y-4">
              <h2 className="mt-0">Thông tin ca thi</h2>
              <label className="block text-sm font-semibold">
                Tên ca thi
                <Input className="mt-2" value={state.title} maxLength={255} onChange={(event) => set("title", event.target.value)} />
              </label>
              <label className="block text-sm font-semibold">
                Mô tả
                <textarea
                  className="mt-2 min-h-28 w-full rounded-lg border border-line bg-surface p-3 text-sm"
                  value={state.description}
                  maxLength={4000}
                  onChange={(event) => set("description", event.target.value)}
                />
              </label>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="block text-sm font-semibold">
                  Bắt đầu
                  <Input className="mt-2" type="datetime-local" value={state.startAtLocal} onChange={(event) => set("startAtLocal", event.target.value)} />
                </label>
                <label className="block text-sm font-semibold">
                  Thời lượng (phút)
                  <Input className="mt-2" type="number" min={1} value={state.durationMinutes} onChange={(event) => set("durationMinutes", Number(event.target.value))} />
                </label>
                <label className="block text-sm font-semibold">
                  Cho vào sớm (phút)
                  <Input className="mt-2" type="number" min={0} value={state.joinBeforeMinutes} onChange={(event) => set("joinBeforeMinutes", Number(event.target.value))} />
                </label>
                <label className="block text-sm font-semibold">
                  Cho vào muộn (phút)
                  <Input className="mt-2" type="number" min={0} value={state.joinAfterMinutes} onChange={(event) => set("joinAfterMinutes", Number(event.target.value))} />
                </label>
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-5">
              <h2 className="mt-0">Bộ câu hỏi và quota</h2>
              <label className="block text-sm font-semibold">
                Bộ câu hỏi
                <Select className="mt-2 w-full" value={state.collectionId} onChange={(event) => set("collectionId", event.target.value)}>
                  <option value="">Chọn bộ câu hỏi</option>
                  {collections.data?.content.map((item) => (
                    <option key={item.id} value={item.id}>{item.name} ({item.stats.questionCount} câu)</option>
                  ))}
                </Select>
              </label>
              {questionCounts.map(({ key, label, capacityKey }) => (
                <label key={key} className="grid grid-cols-[1fr_120px_auto] items-center gap-3">
                  <span>{label}</span>
                  <Input type="number" min={0} value={state[key]} onChange={(event) => set(key, Number(event.target.value))} />
                  <span className="text-sm text-muted">/ {capacity?.[capacityKey] ?? 0}</span>
                </label>
              ))}
            </div>
          )}

          {step === 3 && (
            <StudentAssignmentStep
              selected={state.selectedStudents}
              onChange={(selectedStudents) => set("selectedStudents", selectedStudents)}
              validationError={validationErrors.find((message) => message.includes("học sinh"))}
            />
          )}

          {step === 4 && (
            <div className="space-y-5">
              <h2 className="mt-0">Chính sách ca thi</h2>
              <fieldset className="space-y-3 rounded-lg border border-line p-4">
                <legend className="px-2 font-semibold">Trộn nội dung</legend>
                <Toggle label="Trộn thứ tự câu hỏi" checked={state.shuffleQuestions} onChange={(value) => set("shuffleQuestions", value)} />
                <Toggle label="Trộn thứ tự đáp án" checked={state.shuffleOptions} onChange={(value) => set("shuffleOptions", value)} />
              </fieldset>
              <div className="grid gap-4 md:grid-cols-2">
                <label className="text-sm font-semibold">
                  Hiển thị kết quả
                  <Select className="mt-2 w-full" value={state.showResultPolicy} onChange={(event) => set("showResultPolicy", event.target.value as ExamBuilderState["showResultPolicy"])}>
                    <option value="NEVER">Không hiển thị</option>
                    <option value="AFTER_SUBMIT">Sau khi nộp</option>
                    <option value="AFTER_CLOSED">Sau khi ca thi kết thúc</option>
                  </Select>
                </label>
                <label className="text-sm font-semibold">
                  Xử lý khi vượt ngưỡng
                  <Select className="mt-2 w-full" value={state.handleViolation} onChange={(event) => set("handleViolation", event.target.value as ExamBuilderState["handleViolation"])}>
                    <option value="WARN">Cảnh báo</option>
                    <option value="PAUSE">Tạm dừng</option>
                    <option value="LOCK">Khóa bài</option>
                  </Select>
                </label>
                <label className="text-sm font-semibold">
                  Ngưỡng vi phạm
                  <Input className="mt-2" type="number" min={0} value={state.maxViolationAllowed} onChange={(event) => set("maxViolationAllowed", Number(event.target.value))} />
                </label>
              </div>
              <Toggle label="Tự động nộp khi hết giờ" checked={state.autoSubmit} onChange={(value) => set("autoSubmit", value)} />
              <Toggle label="Yêu cầu chế độ toàn màn hình" checked={state.requireFullscreen} onChange={(value) => set("requireFullscreen", value)} />
            </div>
          )}

          {step === 5 && (
            <div className="space-y-5">
              <h2 className="mt-0">Xem lại cấu hình</h2>
              <dl className="grid gap-4 sm:grid-cols-2">
                <Summary label="Tên ca thi" value={state.title || "Chưa nhập"} />
                <Summary label="Bộ câu hỏi" value={selectedCollection?.name ?? "Chưa chọn"} />
                <Summary label="Số câu" value={`${total} câu (${state.easyCount}/${state.mediumCount}/${state.hardCount})`} />
                <Summary label="Học sinh" value={`${state.selectedStudents.size} học sinh`} />
                <Summary label="Bắt đầu" value={state.startAtLocal || "Chưa chọn"} />
                <Summary label="Thời lượng" value={`${state.durationMinutes} phút`} />
              </dl>
            </div>
          )}

          <div className="mt-8 flex justify-between gap-3">
            <Button variant="secondary" disabled={step === 1 || submitting} onClick={() => setStep((value) => value - 1)}>Quay lại</Button>
            {step < 5 ? (
              <Button
                disabled={submitting}
                onClick={() => {
                  if (validateStep(step as ExamBuilderStep)) setStep((value) => value + 1);
                }}
              >
                Tiếp tục
              </Button>
            ) : (
              <Button loading={submitting} onClick={submit}>{submitLabel}</Button>
            )}
          </div>
        </section>

        <aside className="h-fit rounded-xl border border-line bg-surface p-6 shadow-soft">
          <h2 className="mt-0">Tóm tắt</h2>
          <p className="font-semibold">{state.title || "Ca thi chưa đặt tên"}</p>
          <p className="text-sm text-muted">{total} câu · {state.durationMinutes} phút · {state.selectedStudents.size} học sinh</p>
          <div className="mt-5 flex h-3 overflow-hidden rounded-full bg-line">
            <span className="bg-success" style={{ width: `${total ? (state.easyCount / total) * 100 : 0}%` }} />
            <span className="bg-warning" style={{ width: `${total ? (state.mediumCount / total) * 100 : 0}%` }} />
            <span className="bg-danger" style={{ width: `${total ? (state.hardCount / total) * 100 : 0}%` }} />
          </div>
        </aside>
      </div>
    </div>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex items-center justify-between gap-4 rounded-lg border border-line p-4">
      <span className="font-semibold">{label}</span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-sm text-muted">{label}</dt><dd className="m-0 font-semibold">{value}</dd></div>;
}

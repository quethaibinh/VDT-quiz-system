import { Save } from "lucide-react";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { DataState } from "@/components/shared/data-state";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { listCollections } from "@/features/teacher/collections/api/collection-api";
import type { LiveQuizDetail, LiveQuizRequest } from "@/features/teacher/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

interface LiveQuizFormProps {
  subjectId: string;
  initial?: LiveQuizDetail;
  submitting?: boolean;
  error?: string | null;
  submitLabel: string;
  onSubmit: (input: LiveQuizRequest) => void;
}

export function LiveQuizForm({
  subjectId,
  initial,
  submitting,
  error,
  submitLabel,
  onSubmit,
}: LiveQuizFormProps) {
  const collections = useQuery({
    queryKey: ["teacher", "subjects", subjectId, "collections", "live-quiz-picker"],
    queryFn: () => listCollections(subjectId, { page: 0, size: 100, status: "ACTIVE" }),
    enabled: Boolean(subjectId),
  });
  const [title, setTitle] = useState(initial?.title ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [collectionId, setCollectionId] = useState(initial?.collectionId ?? "");
  const [shuffleQuestions, setShuffleQuestions] = useState(initial?.shuffleQuestions ?? true);
  const [showLeaderboard, setShowLeaderboard] = useState(initial?.showLeaderboard ?? true);
  const [showCorrectAnswer, setShowCorrectAnswer] = useState(initial?.showCorrectAnswer ?? false);

  const effectiveCollectionId = collectionId || collections.data?.content[0]?.id || "";
  const selectedCollection = collections.data?.content.find((item) => item.id === effectiveCollectionId);
  const canSubmit = title.trim().length > 0 && Boolean(effectiveCollectionId) && !submitting;

  return (
    <DataState
      loading={collections.isLoading}
      error={collections.error ? getApiErrorMessage(collections.error) : null}
      empty={collections.data?.content.length === 0}
      emptyMessage="Môn học này chưa có bộ câu hỏi đang hoạt động để tạo quiz."
      onRetry={() => collections.refetch()}
    >
      <form
        className="space-y-6"
        onSubmit={(event) => {
          event.preventDefault();
          if (!canSubmit) return;
          onSubmit({
            title: title.trim(),
            description: description.trim() || null,
            collectionId: effectiveCollectionId,
            shuffleQuestions,
            showLeaderboard,
            showCorrectAnswer,
            joinPolicy: "CODE_ONLY",
          });
        }}
      >
        <section className="grid gap-5 rounded-xl border border-line bg-surface p-5 shadow-soft lg:grid-cols-2">
          <label className="flex flex-col text-sm font-semibold lg:col-span-2">
            Tên quiz
            <Input
              className="mt-2"
              value={title}
              maxLength={255}
              placeholder="Ví dụ: Ôn tập chương 3"
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>

          <label className="flex flex-col text-sm font-semibold lg:col-span-2">
            Mô tả
            <textarea
              className="mt-2 min-h-28 rounded-lg border border-line bg-surface px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
              value={description}
              maxLength={4000}
              placeholder="Ghi chú ngắn cho giáo viên..."
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>

          <label className="flex flex-col text-sm font-semibold">
            Bộ câu hỏi
            <Select
              className="mt-2"
              value={effectiveCollectionId}
              onChange={(event) => setCollectionId(event.target.value)}
            >
              {collections.data?.content.map((collection) => (
                <option key={collection.id} value={collection.id}>
                  {collection.name}
                </option>
              ))}
            </Select>
          </label>

          <div className="rounded-lg border border-line p-4 text-sm">
            <p className="m-0 font-semibold text-ink">Snapshot câu hỏi</p>
            <p className="m-0 mt-2 text-muted">
              {selectedCollection
                ? `${selectedCollection.stats.questionCount} câu · Dễ ${selectedCollection.stats.easy} · TB ${selectedCollection.stats.medium} · Khó ${selectedCollection.stats.hard}`
                : "Chọn bộ câu hỏi để xem thống kê."}
            </p>
          </div>

          <Toggle
            label="Trộn thứ tự câu hỏi"
            description="Mỗi lần chơi có thể nhận thứ tự câu hỏi khác nhau."
            checked={shuffleQuestions}
            onChange={setShuffleQuestions}
          />
          <Toggle
            label="Hiện bảng xếp hạng"
            description="Dùng cho lớp học muốn xem điểm theo thời gian thực."
            checked={showLeaderboard}
            onChange={setShowLeaderboard}
          />
          <Toggle
            label="Hiện đáp án đúng"
            description="Phase này chỉ lưu cấu hình, student play sẽ xử lý sau."
            checked={showCorrectAnswer}
            onChange={setShowCorrectAnswer}
          />
          <div className="rounded-lg border border-line p-4 text-sm">
            <p className="m-0 font-semibold text-ink">Join policy</p>
            <p className="m-0 mt-2 text-muted">CODE_ONLY · Học sinh vào phòng bằng mã 6 ký tự.</p>
          </div>
        </section>

        {error && <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger">{error}</p>}

        <div className="flex justify-end">
          <Button type="submit" loading={submitting} disabled={!canSubmit}>
            <Save className="h-4 w-4" />
            {submitLabel}
          </Button>
        </div>
      </form>
    </DataState>
  );
}

function Toggle({
  label,
  description,
  checked,
  onChange,
}: {
  label: string;
  description: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className="flex min-h-28 cursor-pointer items-start gap-3 rounded-lg border border-line p-4">
      <input
        type="checkbox"
        className="mt-1 h-4 w-4 accent-primary"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span>
        <span className="block font-semibold text-ink">{label}</span>
        <span className="mt-1 block text-sm text-muted">{description}</span>
      </span>
    </label>
  );
}

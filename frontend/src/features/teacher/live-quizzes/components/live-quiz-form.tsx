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
      emptyMessage="Mon hoc nay chua co bo cau hoi dang hoat dong de tao quiz."
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
            Ten quiz
            <Input
              className="mt-2"
              value={title}
              maxLength={255}
              placeholder="Vi du: On tap chuong 3"
              onChange={(event) => setTitle(event.target.value)}
            />
          </label>

          <label className="flex flex-col text-sm font-semibold lg:col-span-2">
            Mo ta
            <textarea
              className="mt-2 min-h-28 rounded-lg border border-line bg-surface px-3 py-2 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
              value={description}
              maxLength={4000}
              placeholder="Ghi chu ngan cho giao vien..."
              onChange={(event) => setDescription(event.target.value)}
            />
          </label>

          <label className="flex flex-col text-sm font-semibold">
            Bo cau hoi
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
            <p className="m-0 font-semibold text-ink">Snapshot cau hoi</p>
            <p className="m-0 mt-2 text-muted">
              {selectedCollection
                ? `${selectedCollection.stats.questionCount} cau · De ${selectedCollection.stats.easy} · TB ${selectedCollection.stats.medium} · Kho ${selectedCollection.stats.hard}`
                : "Chon bo cau hoi de xem thong ke."}
            </p>
          </div>

          <Toggle
            label="Tron thu tu cau hoi"
            description="Moi lan choi co the nhan thu tu cau hoi khac nhau."
            checked={shuffleQuestions}
            onChange={setShuffleQuestions}
          />
          <Toggle
            label="Hien bang xep hang"
            description="Dung cho lop hoc muon xem diem theo thoi gian thuc."
            checked={showLeaderboard}
            onChange={setShowLeaderboard}
          />
          <Toggle
            label="Hien dap an dung"
            description="Phase nay chi luu cau hinh, student play se xu ly sau."
            checked={showCorrectAnswer}
            onChange={setShowCorrectAnswer}
          />
          <div className="rounded-lg border border-line p-4 text-sm">
            <p className="m-0 font-semibold text-ink">Join policy</p>
            <p className="m-0 mt-2 text-muted">CODE_ONLY · Hoc sinh vao phong bang ma 6 ky tu.</p>
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

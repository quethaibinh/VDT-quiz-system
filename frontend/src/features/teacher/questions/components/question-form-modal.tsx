import { zodResolver } from "@hookform/resolvers/zod";
import * as Dialog from "@radix-ui/react-dialog";
import { useQuery } from "@tanstack/react-query";
import { Image, Plus, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useFieldArray, useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { getQuestionImageUrl, listTopics, uploadQuestionImage } from "@/features/teacher/questions/api/question-api";
import type { QuestionDetail, QuestionInput } from "@/features/teacher/questions/model/question-types";

const optionSchema = z.object({
  optionKey: z.string().min(1, "Bắt buộc"),
  content: z.string().min(1, "Nội dung đáp án là bắt buộc."),
  explanation: z.string().optional().nullable(),
  correct: z.boolean(),
});

const questionSchema = z.object({
  topicId: z.string().min(1, "Chủ đề là bắt buộc."),
  questionType: z.enum(["SINGLE_CHOICE", "MULTI_CHOICE"]),
  content: z.string().min(1, "Nội dung câu hỏi là bắt buộc."),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
  defaultScore: z.coerce.number().min(0, "Điểm phải lớn hơn hoặc bằng 0").default(1.0),
  estimatedSecond: z.coerce.number().min(1, "Thời gian phải lớn hơn hoặc bằng 1 giây").default(60),
  visibility: z.enum(["PUBLIC", "PRIVATE"]),
  imageObjectKey: z.string().optional().nullable(),
  explanation: z.string().optional().nullable(),
  options: z.array(optionSchema).min(2, "Phải có ít nhất 2 đáp án."),
});

type FormInput = z.input<typeof questionSchema>;
type FormValues = z.output<typeof questionSchema>;

export function QuestionFormModal({
  open,
  subjectId,
  question,
  loading,
  onSubmit,
  onOpenChange,
}: {
  open: boolean;
  subjectId: string;
  question?: QuestionDetail | null;
  loading?: boolean;
  onSubmit: (value: QuestionInput) => void;
  onOpenChange: (open: boolean) => void;
}) {
  const topics = useQuery({
    queryKey: ["teacher", "topics", subjectId],
    queryFn: () => listTopics(subjectId),
    enabled: open,
  });
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);
  const [imageUploading, setImageUploading] = useState(false);
  const [imageError, setImageError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(questionSchema),
    defaultValues: {
      topicId: "",
      questionType: "SINGLE_CHOICE",
      content: "",
      difficulty: "EASY",
      defaultScore: 1.0,
      estimatedSecond: 60,
      visibility: "PRIVATE",
      explanation: "",
      imageObjectKey: null,
      options: [
        { optionKey: "A", content: "", correct: false, explanation: "" },
        { optionKey: "B", content: "", correct: false, explanation: "" },
      ],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "options",
  });

  const questionType = useWatch({ control, name: "questionType" });
  const optionsValues = useWatch({ control, name: "options" }) ?? [];
  const imageObjectKey = useWatch({ control, name: "imageObjectKey" });

  // Reset form khi question thay đổi (ví dụ khi mở modal sửa)
  useEffect(() => {
    if (question) {
      reset({
        topicId: question.topicId ?? "",
        questionType: question.questionType,
        content: question.content,
        difficulty: question.difficulty,
        defaultScore: question.defaultScore,
        estimatedSecond: question.estimatedSecond,
        visibility: question.visibility,
        explanation: question.explanation ?? "",
        imageObjectKey: question.imageObjectKey ?? null,
        options: question.options.map((opt) => ({
          optionKey: opt.optionKey,
          content: opt.content,
          correct: opt.correct ?? false,
          explanation: opt.explanation ?? "",
        })),
      });
    } else {
      reset({
        topicId: "",
        questionType: "SINGLE_CHOICE",
        content: "",
        difficulty: "EASY",
        defaultScore: 1.0,
        estimatedSecond: 60,
        visibility: "PRIVATE",
        explanation: "",
        imageObjectKey: null,
        options: [
          { optionKey: "A", content: "", correct: false, explanation: "" },
          { optionKey: "B", content: "", correct: false, explanation: "" },
        ],
      });
    }
  }, [question, reset, open]);

  useEffect(() => {
    if (!open || !imageObjectKey) {
      setImagePreviewUrl(null);
      return;
    }
    let cancelled = false;
    getQuestionImageUrl(subjectId, imageObjectKey)
      .then((response) => {
        if (!cancelled) setImagePreviewUrl(response.url);
      })
      .catch(() => {
        if (!cancelled) setImagePreviewUrl(null);
      });
    return () => {
      cancelled = true;
    };
  }, [imageObjectKey, open, subjectId]);

  // Đảm bảo tính nhất quán của SINGLE_CHOICE: chỉ có 1 đáp án đúng
  const handleCorrectChange = (index: number, checked: boolean) => {
    if (questionType === "SINGLE_CHOICE") {
      optionsValues.forEach((_, idx) => {
        setValue(`options.${idx}.correct`, idx === index ? checked : false);
      });
    } else {
      setValue(`options.${index}.correct`, checked);
    }
  };

  const handleAddOption = () => {
    const nextKey = String.fromCharCode(65 + fields.length); // A, B, C, D...
    append({ optionKey: nextKey, content: "", correct: false, explanation: "" });
  };

  const handleImageChange = async (file?: File | null) => {
    if (!file) return;
    setImageUploading(true);
    setImageError(null);
    try {
      const response = await uploadQuestionImage(subjectId, file);
      setValue("imageObjectKey", response.imageObjectKey, { shouldDirty: true });
      setImagePreviewUrl(URL.createObjectURL(file));
    } catch {
      setImageError("Khong the tai anh len. Vui long kiem tra dinh dang va dung luong.");
    } finally {
      setImageUploading(false);
    }
  };

  const handleRemoveImage = () => {
    setValue("imageObjectKey", null, { shouldDirty: true });
    setImagePreviewUrl(null);
    setImageError(null);
  };

  const onFormSubmit = (data: FormValues) => {
    onSubmit({
      topicId: data.topicId,
      questionType: data.questionType,
      content: data.content,
      difficulty: data.difficulty,
      defaultScore: data.defaultScore,
      estimatedSecond: data.estimatedSecond,
      visibility: data.visibility,
      explanation: data.explanation || null,
      imageObjectKey: data.imageObjectKey || null,
      options: data.options.map((opt) => ({
        optionKey: opt.optionKey,
        content: opt.content,
        correct: opt.correct,
        explanation: opt.explanation || null,
      })),
    });
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[min(92vw,640px)] -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-line bg-surface p-6 shadow-soft">
          <Dialog.Title className="m-0 text-3xl font-semibold">
            {question ? "Chỉnh sửa câu hỏi" : "Tạo câu hỏi mới"}
          </Dialog.Title>
          <Dialog.Close className="absolute right-4 top-4 rounded-lg p-2" aria-label="Đóng">
            <X className="h-5 w-5" />
          </Dialog.Close>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit(onFormSubmit)}>
            {/* Chủ đề & Loại */}
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block text-sm font-semibold">
                Chủ đề
                <Select className="mt-2 w-full" {...register("topicId")} disabled={topics.isLoading}>
                  <option value="">-- Chọn chủ đề --</option>
                  {topics.data?.map((topic) => (
                    <option key={topic.id} value={topic.id}>
                      {topic.name}
                    </option>
                  ))}
                </Select>
                {errors.topicId && <p className="mt-1 text-xs text-danger">{errors.topicId.message}</p>}
              </label>

              <label className="block text-sm font-semibold">
                Loại câu hỏi
                <Select
                  className="mt-2 w-full"
                  {...register("questionType")}
                  onChange={(e) => {
                    register("questionType").onChange(e);
                    // Reset correct của toàn bộ đáp án khi đổi loại câu hỏi
                    optionsValues.forEach((_, idx) => setValue(`options.${idx}.correct`, false));
                  }}
                >
                  <option value="SINGLE_CHOICE">Một đáp án (Single Choice)</option>
                  <option value="MULTI_CHOICE">Nhiều đáp án (Multi Choice)</option>
                </Select>
              </label>
            </div>

            {/* Nội dung câu hỏi */}
            <label className="block text-sm font-semibold">
              Nội dung câu hỏi
              <textarea
                className="mt-2 min-h-20 w-full rounded-lg border border-line bg-surface p-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="Nhập nội dung câu hỏi..."
                {...register("content")}
              />
              {errors.content && <p className="mt-1 text-xs text-danger">{errors.content.message}</p>}
            </label>

            <div className="rounded-lg border border-line bg-surface p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2 text-sm font-semibold">
                  <Image className="h-4 w-4 text-primary" />
                  Anh minh hoa
                </div>
                <div className="flex items-center gap-2">
                  {imageObjectKey && (
                    <Button type="button" variant="secondary" onClick={handleRemoveImage}>
                      Xoa anh
                    </Button>
                  )}
                  <label className="inline-flex cursor-pointer items-center rounded-lg border border-line px-3 py-2 text-sm font-semibold hover:bg-ink/5">
                    {imageUploading ? "Dang tai..." : "Chon anh"}
                    <input
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      className="sr-only"
                      disabled={imageUploading}
                      onChange={(event) => handleImageChange(event.target.files?.[0])}
                    />
                  </label>
                </div>
              </div>
              {imagePreviewUrl && (
                <div className="mt-3 max-h-64 overflow-hidden rounded-lg border border-line bg-white">
                  <img src={imagePreviewUrl} alt="" className="max-h-64 w-full object-contain" />
                </div>
              )}
              {imageError && <p className="mt-2 text-xs text-danger">{imageError}</p>}
            </div>

            {/* Thông số câu hỏi */}
            <div className="grid gap-4 sm:grid-cols-4">
              <label className="block text-sm font-semibold">
                Độ khó
                <Select className="mt-2 w-full" {...register("difficulty")}>
                  <option value="EASY">Dễ</option>
                  <option value="MEDIUM">Vừa</option>
                  <option value="HARD">Khó</option>
                </Select>
              </label>

              <label className="block text-sm font-semibold">
                Điểm mặc định
                <Input type="number" step="0.1" className="mt-2 w-full" {...register("defaultScore")} />
                {errors.defaultScore && <p className="mt-1 text-xs text-danger">{errors.defaultScore.message}</p>}
              </label>

              <label className="block text-sm font-semibold">
                Thời gian (giây)
                <Input type="number" className="mt-2 w-full" {...register("estimatedSecond")} />
                {errors.estimatedSecond && <p className="mt-1 text-xs text-danger">{errors.estimatedSecond.message}</p>}
              </label>

              <label className="block text-sm font-semibold">
                Hiển thị
                <Select className="mt-2 w-full" {...register("visibility")}>
                  <option value="PRIVATE">Riêng tư</option>
                  <option value="PUBLIC">Công khai</option>
                </Select>
              </label>
            </div>

            {/* Giải thích câu hỏi */}
            <label className="block text-sm font-semibold">
              Giải thích câu hỏi (Không bắt buộc)
              <textarea
                className="mt-2 min-h-16 w-full rounded-lg border border-line bg-surface p-3 text-sm focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="Nhập phần giải thích cho câu hỏi..."
                {...register("explanation")}
              />
            </label>

            {/* Danh sách đáp án */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold">Danh sách đáp án</span>
                <Button type="button" variant="secondary" onClick={handleAddOption}>
                  <Plus className="h-4 w-4" /> Thêm đáp án
                </Button>
              </div>
              {errors.options && <p className="text-xs text-danger">{errors.options.message}</p>}

              <div className="max-h-[300px] space-y-3 overflow-y-auto pr-1">
                {fields.map((field, index) => {
                  const optionKey = String.fromCharCode(65 + index);
                  return (
                    <div key={field.id} className="rounded-xl border border-line bg-surface p-4 space-y-3 shadow-sm">
                      <div className="flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3">
                          {/* Ô tích chọn đáp án đúng */}
                          <input
                            type={questionType === "SINGLE_CHOICE" ? "radio" : "checkbox"}
                            name="correct-answer"
                            checked={optionsValues?.[index]?.correct ?? false}
                            onChange={(e) => handleCorrectChange(index, e.target.checked)}
                            className="h-4 w-4 cursor-pointer text-primary"
                            aria-label={`Đáp án đúng ${optionKey}`}
                          />
                          <span className="text-sm font-bold text-primary">Đáp án {optionKey}</span>
                        </div>

                        {/* Xóa đáp án */}
                        {fields.length > 2 && (
                          <button
                            type="button"
                            onClick={() => remove(index)}
                            className="rounded p-1 text-danger hover:bg-danger/10"
                            title="Xóa đáp án"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        )}
                      </div>

                      {/* Nội dung đáp án */}
                      <Input
                        placeholder={`Nhập nội dung đáp án ${optionKey}...`}
                        {...register(`options.${index}.content` as const)}
                      />
                      {errors.options?.[index]?.content && (
                        <p className="text-xs text-danger">{errors.options[index].content.message}</p>
                      )}

                      {/* Giải thích đáp án */}
                      <Input
                        placeholder="Giải thích đáp án này (Không bắt buộc)..."
                        {...register(`options.${index}.explanation` as const)}
                        className="text-xs"
                      />
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Các nút điều khiển */}
            <div className="mt-6 flex justify-end gap-2 border-t border-line pt-4">
              <Dialog.Close asChild>
                <Button type="button" variant="secondary">
                  Hủy
                </Button>
              </Dialog.Close>
              <Button type="submit" loading={loading}>
                {question ? "Cập nhật câu hỏi" : "Tạo câu hỏi"}
              </Button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

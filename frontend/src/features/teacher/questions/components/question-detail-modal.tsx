import * as Dialog from "@radix-ui/react-dialog";
import { useQuery } from "@tanstack/react-query";
import { CheckCircle2, Clock, FileText, HelpCircle, Info, ShieldAlert, Award, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getQuestionDetail, getQuestionImageUrl, listTopics } from "@/features/teacher/questions/api/question-api";

export function QuestionDetailModal({
  open,
  subjectId,
  questionId,
  onOpenChange,
}: {
  open: boolean;
  subjectId: string;
  questionId: string | null;
  onOpenChange: (open: boolean) => void;
}) {
  const { data: question, isLoading } = useQuery({
    queryKey: ["teacher", "question", subjectId, questionId],
    queryFn: () => getQuestionDetail(subjectId, questionId!),
    enabled: open && !!questionId,
  });

  const { data: topics } = useQuery({
    queryKey: ["teacher", "topics", subjectId],
    queryFn: () => listTopics(subjectId),
    enabled: open,
  });

  const { data: imageUrl } = useQuery({
    queryKey: ["teacher", "question-image-url", subjectId, question?.imageObjectKey],
    queryFn: () => getQuestionImageUrl(subjectId, question!.imageObjectKey!).then((value) => value.url),
    enabled: open && !!question?.imageObjectKey,
  });

  const topicName = topics?.find((t) => t.id === question?.topicId)?.name || "Chưa phân loại";

  const getDifficultyBadge = (difficulty?: string) => {
    switch (difficulty) {
      case "EASY":
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400">Dễ</span>;
      case "MEDIUM":
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-700 dark:bg-amber-950/30 dark:text-amber-400">Trung bình</span>;
      case "HARD":
        return <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-950/30 dark:text-rose-400">Khó</span>;
      default:
        return null;
    }
  };

  const getVisibilityBadge = (visibility?: string) => {
    return visibility === "PUBLIC" ? (
      <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700 dark:bg-blue-950/30 dark:text-blue-400">Công khai</span>
    ) : (
      <span className="inline-flex items-center gap-1 rounded-full bg-slate-50 px-2.5 py-0.5 text-xs font-semibold text-slate-700 dark:bg-slate-850/30 dark:text-slate-400">Riêng tư</span>
    );
  };

  const getStatusBadge = (status?: string) => {
    return status === "ACTIVE" ? (
      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:bg-emerald-950/30 dark:text-emerald-400">Đang hoạt động</span>
    ) : (
      <span className="inline-flex items-center gap-1 rounded-full bg-rose-50 px-2.5 py-0.5 text-xs font-semibold text-rose-700 dark:bg-rose-950/30 dark:text-rose-400">Đã lưu trữ</span>
    );
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35 transition-opacity duration-300" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 max-h-[90vh] w-[min(92vw,680px)] -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border border-line bg-surface p-6 shadow-soft transition-all duration-300">
          <Dialog.Title className="m-0 text-3xl font-semibold flex items-center gap-2">
            <FileText className="h-6 w-6 text-primary" />
            Chi tiết câu hỏi
          </Dialog.Title>
          <Dialog.Close className="absolute right-4 top-4 rounded-lg p-2 text-muted hover:bg-slate-100 hover:text-ink transition-colors" aria-label="Đóng">
            <X className="h-5 w-5" />
          </Dialog.Close>

          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 space-y-4">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
              <p className="text-sm text-muted">Đang tải thông tin chi tiết...</p>
            </div>
          ) : !question ? (
            <div className="flex flex-col items-center justify-center py-12 text-center text-muted">
              <ShieldAlert className="h-12 w-12 text-warning mb-2" />
              <p className="font-semibold">Không tìm thấy thông tin câu hỏi</p>
            </div>
          ) : (
            <div className="mt-6 space-y-6">
              {/* Metadata Grid */}
              <div className="grid grid-cols-2 gap-4 rounded-xl bg-slate-50 dark:bg-slate-900/40 p-4 text-sm">
                <div>
                  <span className="text-muted block text-xs">Môn học</span>
                  <span className="font-medium">{topicName}</span>
                </div>
                <div>
                  <span className="text-muted block text-xs">Loại câu hỏi</span>
                  <span className="font-medium">
                    {question.questionType === "SINGLE_CHOICE" ? "Một đáp án (Single Choice)" : "Nhiều đáp án (Multi Choice)"}
                  </span>
                </div>
                <div>
                  <span className="text-muted block text-xs flex items-center gap-1">
                    <Clock className="h-3 w-3" /> Thời gian ước tính
                  </span>
                  <span className="font-medium">{question.estimatedSecond} giây</span>
                </div>
                <div>
                  <span className="text-muted block text-xs flex items-center gap-1">
                    <Award className="h-3 w-3" /> Điểm mặc định
                  </span>
                  <span className="font-medium">{question.defaultScore} điểm</span>
                </div>
                <div className="col-span-2 flex flex-wrap gap-2 pt-2 border-t border-line">
                  {getDifficultyBadge(question.difficulty)}
                  {getVisibilityBadge(question.visibility)}
                  {getStatusBadge(question.status)}
                  <span className="inline-flex items-center gap-1 rounded-full bg-purple-50 px-2.5 py-0.5 text-xs font-semibold text-purple-700 dark:bg-purple-950/30 dark:text-purple-400">
                    Nguồn: {question.source === "EXCEL" ? "Nhập từ Excel" : "Tạo thủ công"}
                  </span>
                </div>
              </div>

              {/* Question Content */}
              <div className="space-y-2">
                <h3 className="text-sm font-semibold flex items-center gap-1.5 text-ink">
                  <HelpCircle className="h-4.5 w-4.5 text-primary" />
                  Nội dung câu hỏi
                </h3>
                <div className="rounded-xl border border-line bg-surface p-4 text-sm font-medium leading-relaxed whitespace-pre-wrap">
                  {question.content}
                </div>
                {imageUrl && (
                  <div className="overflow-hidden rounded-xl border border-line bg-white">
                    <img src={imageUrl} alt="" className="max-h-80 w-full object-contain" loading="lazy" />
                  </div>
                )}
              </div>

              {/* Options List */}
              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-ink">Danh sách lựa chọn</h3>
                <div className="space-y-2">
                  {question.options.map((opt) => (
                    <div
                      key={opt.id}
                      className={`flex flex-col gap-1 rounded-xl border p-4 transition-colors ${
                        opt.correct
                          ? "border-emerald-500 bg-emerald-50/35 dark:bg-emerald-950/10"
                          : "border-line bg-surface hover:border-slate-350"
                      }`}
                    >
                      <div className="flex items-start gap-3 justify-between">
                        <div className="flex items-start gap-3">
                          <span
                            className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                              opt.correct
                                ? "bg-emerald-550 text-white"
                                : "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            }`}
                          >
                            {opt.optionKey}
                          </span>
                          <p className="text-sm font-medium leading-relaxed">{opt.content}</p>
                        </div>
                        {opt.correct && (
                          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-550" />
                        )}
                      </div>
                      {opt.explanation && (
                        <div className="mt-2 pl-9 text-xs text-muted flex items-start gap-1">
                          <Info className="h-3.5 w-3.5 shrink-0 text-slate-400 mt-0.5" />
                          <span>
                            <span className="font-semibold">Giải thích:</span> {opt.explanation}
                          </span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              {/* Question-level Explanation */}
              {question.explanation && (
                <div className="space-y-2">
                  <h3 className="text-sm font-semibold flex items-center gap-1.5 text-ink">
                    <Info className="h-4.5 w-4.5 text-primary" />
                    Giải thích tổng quan
                  </h3>
                  <div className="rounded-xl border border-line bg-blue-50/20 dark:bg-blue-950/5 p-4 text-sm leading-relaxed whitespace-pre-wrap">
                    {question.explanation}
                  </div>
                </div>
              )}

              {/* Action buttons */}
              <div className="mt-6 flex justify-end gap-2 border-t border-line pt-4">
                <Dialog.Close asChild>
                  <Button type="button" variant="secondary">
                    Đóng
                  </Button>
                </Dialog.Close>
              </div>
            </div>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

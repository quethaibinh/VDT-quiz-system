import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FileUp, Plus } from "lucide-react";
import { useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Button } from "@/components/ui/button";
import { QuestionFilters, type QuestionFilterValue } from "@/features/teacher/questions/components/question-filters";
import { QuestionList } from "@/features/teacher/questions/components/question-list";
import { QuestionFormModal } from "@/features/teacher/questions/components/question-form-modal";
import { QuestionDetailModal } from "@/features/teacher/questions/components/question-detail-modal";
import {
  searchQuestions,
  createQuestion,
  getQuestionDetail,
  updateQuestion,
  archiveQuestion,
  restoreQuestion,
} from "@/features/teacher/questions/api/question-api";
import { getApiErrorMessage } from "@/lib/http/api-error";
import type { QuestionInput } from "@/features/teacher/questions/model/question-types";

export function QuestionBankPage() {
  const { subjectId = "" } = useParams();
  const [params, setParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Modal states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [viewingId, setViewingId] = useState<string | null>(null);

  const value: QuestionFilterValue = {
    keyword: params.get("keyword") ?? "",
    difficulty: params.get("difficulty") ?? "",
    visibility: params.get("visibility") ?? "",
    ownerScope: params.get("ownerScope") ?? "ALL",
    questionType: params.get("questionType") ?? "",
  };
  const page = Number(params.get("page") ?? 0);

  const query = useQuery({
    queryKey: ["teacher", "questions", subjectId, Object.fromEntries(params)],
    queryFn: () => searchQuestions(subjectId, { ...value, page, size: 20, sort: "createdAt,desc" }),
  });

  // Fetch question details for edit mode
  const editingQuestionQuery = useQuery({
    queryKey: ["teacher", "question", subjectId, editingId],
    queryFn: () => getQuestionDetail(subjectId, editingId!),
    enabled: isFormOpen && !!editingId,
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: (input: QuestionInput) => createQuestion(subjectId, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "questions", subjectId] });
      setIsFormOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (input: QuestionInput) => updateQuestion(subjectId, editingId!, input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "questions", subjectId] });
      queryClient.invalidateQueries({ queryKey: ["teacher", "question", subjectId, editingId] });
      setIsFormOpen(false);
      setEditingId(null);
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) => archiveQuestion(subjectId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "questions", subjectId] });
    },
  });

  const restoreMutation = useMutation({
    mutationFn: (id: string) => restoreQuestion(subjectId, id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["teacher", "questions", subjectId] });
    },
  });

  const updateFilters = (next: QuestionFilterValue) => {
    const values = Object.entries(next).filter(([, item]) => item);
    setParams({ ...Object.fromEntries(values), page: "0" });
  };

  const handleFormSubmit = (input: QuestionInput) => {
    // Re-map correct/optionKeys to make sure everything matches
    const mappedOptions = input.options.map((opt, idx) => ({
      ...opt,
      optionKey: String.fromCharCode(65 + idx),
    }));
    const payload = { ...input, options: mappedOptions };

    if (editingId) {
      updateMutation.mutate(payload);
    } else {
      createMutation.mutate(payload);
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Ngân hàng câu hỏi"
        description="Tìm kiếm và chọn câu hỏi trong môn học."
        action={
          <div className="flex gap-2">
            <Button
              onClick={() => {
                setEditingId(null);
                setIsFormOpen(true);
              }}
            >
              <Plus className="h-4 w-4" /> Tạo câu hỏi
            </Button>
            <Link to="import">
              <Button variant="secondary">
                <FileUp className="h-4 w-4" /> Import Excel
              </Button>
            </Link>
          </div>
        }
      />
      <QuestionFilters value={value} onChange={updateFilters} />
      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={!query.data?.content.length}
        onRetry={() => query.refetch()}
      >
        {query.data && (
          <div className="space-y-4">
            <QuestionList
              questions={query.data.content}
              onView={(id) => {
                setViewingId(id);
                setIsDetailOpen(true);
              }}
              onEdit={(id) => {
                setEditingId(id);
                setIsFormOpen(true);
              }}
              onArchive={(id) => archiveMutation.mutate(id)}
              onRestore={(id) => restoreMutation.mutate(id)}
            />
            <Pagination
              page={query.data.page}
              totalPages={query.data.totalPages}
              onChange={(next) => {
                params.set("page", String(next));
                setParams(params);
              }}
            />
          </div>
        )}
      </DataState>

      {/* Form Modal (Create/Edit) */}
      <QuestionFormModal
        open={isFormOpen}
        subjectId={subjectId}
        question={editingId ? editingQuestionQuery.data : null}
        loading={
          editingId
            ? editingQuestionQuery.isLoading || updateMutation.isPending
            : createMutation.isPending
        }
        onSubmit={handleFormSubmit}
        onOpenChange={(open) => {
          setIsFormOpen(open);
          if (!open) {
            setEditingId(null);
          }
        }}
      />

      {/* Detail Modal */}
      <QuestionDetailModal
        open={isDetailOpen}
        subjectId={subjectId}
        questionId={viewingId}
        onOpenChange={(open) => {
          setIsDetailOpen(open);
          if (!open) {
            setViewingId(null);
          }
        }}
      />
    </div>
  );
}


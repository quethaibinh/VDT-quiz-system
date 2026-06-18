import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, Plus, RotateCcw, Trash2 } from "lucide-react";
import { useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { addQuestions, addQuestionsByFilter, archiveCollection, getCollection, listCollectionQuestions, removeQuestions, restoreCollection, updateCollection } from "@/features/teacher/collections/api/collection-api";
import { CollectionForm } from "@/features/teacher/collections/components/collection-form";
import type { CollectionInput } from "@/features/teacher/collections/model/collection-types";
import { QuestionFilters, QuestionList, searchQuestions, type QuestionFilterValue } from "@/features/teacher/questions";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function CollectionDetailPage() {
  const { subjectId = "", collectionId = "" } = useParams();
  const [search, setSearch] = useSearchParams();
  const [editing, setEditing] = useState(false);
  const [confirmAction, setConfirmAction] = useState<"archive" | "add-all" | "remove" | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [fetchingAll, setFetchingAll] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const tab = search.get("tab") ?? "inside";
  const filter: QuestionFilterValue = { keyword: search.get("keyword") ?? "", difficulty: search.get("difficulty") ?? "", visibility: search.get("visibility") ?? "", ownerScope: search.get("ownerScope") ?? "ALL", questionType: search.get("questionType") ?? "" };
  const page = Number(search.get("page") ?? 0);
  const detail = useQuery({ queryKey: ["teacher", "collection", subjectId, collectionId], queryFn: () => getCollection(subjectId, collectionId) });
  const questions = useQuery({
    queryKey: ["teacher", "collection-questions", subjectId, collectionId, tab, filter, page],
    queryFn: () => tab === "inside"
      ? listCollectionQuestions(subjectId, collectionId, { ...filter, page, size: 20, sort: "createdAt,desc" })
      : searchQuestions(subjectId, { ...filter, collectionId, membership: "NOT_IN", page, size: 20, sort: "createdAt,desc" }),
    enabled: Boolean(detail.data),
  });
  const invalidate = () => { setSelected(new Set()); setConfirmAction(null); queryClient.invalidateQueries({ queryKey: ["teacher", "collection", subjectId, collectionId] }); queryClient.invalidateQueries({ queryKey: ["teacher", "collection-questions", subjectId, collectionId] }); queryClient.invalidateQueries({ queryKey: ["teacher", "collections", subjectId] }); };
  const update = useMutation({ mutationFn: (input: CollectionInput) => updateCollection(subjectId, collectionId, input), onSuccess: () => { setEditing(false); invalidate(); } });
  const archive = useMutation({ mutationFn: () => archiveCollection(subjectId, collectionId), onSuccess: () => navigate("..") });
  const restore = useMutation({ mutationFn: () => restoreCollection(subjectId, collectionId), onSuccess: invalidate });
  const add = useMutation({ mutationFn: () => addQuestions(subjectId, collectionId, [...selected]), onSuccess: invalidate });
  const remove = useMutation({ mutationFn: () => removeQuestions(subjectId, collectionId, [...selected]), onSuccess: invalidate });
  const addAll = useMutation({ mutationFn: () => addQuestionsByFilter(subjectId, collectionId, { keyword: filter.keyword || null, topicId: null, difficulties: filter.difficulty ? [filter.difficulty] : [], visibilities: filter.visibility ? [filter.visibility] : [], ownerScope: filter.ownerScope, questionType: filter.questionType || null, excludeQuestionIds: [] }), onSuccess: invalidate });
  const pendingError = [update.error, archive.error, restore.error, add.error, remove.error, addAll.error].find(Boolean);
  const toggle = (id: string) => setSelected((current) => {
    const next = new Set(current);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    return next;
  });
  const handleToggleAll = async () => {
    if (!questions.data) return;
    const currentPageIds = questions.data.content.map((q) => q.id);
    const allCurrentSelected = currentPageIds.every((id) => selected.has(id));

    if (allCurrentSelected) {
      setFetchingAll(true);
      try {
        const total = questions.data.totalElements;
        let allIds: string[] = [];
        if (total <= questions.data.content.length) {
          allIds = currentPageIds;
        } else {
          const fetchSize = Math.min(total, 1000);
          if (tab === "inside") {
            const res = await listCollectionQuestions(subjectId, collectionId, { ...filter, page: 0, size: fetchSize, sort: "createdAt,desc" });
            allIds = res.content.map((q) => q.id);
          } else {
            const res = await searchQuestions(subjectId, { ...filter, collectionId, membership: "NOT_IN", page: 0, size: fetchSize, sort: "createdAt,desc" });
            allIds = res.content.map((q) => q.id);
          }
        }
        setSelected((prev) => {
          const next = new Set(prev);
          allIds.forEach((id) => next.delete(id));
          return next;
        });
      } catch (err) {
        console.error("Failed to deselect all questions", err);
      } finally {
        setFetchingAll(false);
      }
    } else {
      setFetchingAll(true);
      try {
        const total = questions.data.totalElements;
        let allIds: string[] = [];
        if (total <= questions.data.content.length) {
          allIds = currentPageIds;
        } else {
          const fetchSize = Math.min(total, 1000);
          if (tab === "inside") {
            const res = await listCollectionQuestions(subjectId, collectionId, { ...filter, page: 0, size: fetchSize, sort: "createdAt,desc" });
            allIds = res.content.map((q) => q.id);
          } else {
            const res = await searchQuestions(subjectId, { ...filter, collectionId, membership: "NOT_IN", page: 0, size: fetchSize, sort: "createdAt,desc" });
            allIds = res.content.map((q) => q.id);
          }
        }
        setSelected((prev) => {
          const next = new Set(prev);
          allIds.forEach((id) => next.add(id));
          return next;
        });
      } catch (err) {
        console.error("Failed to select all questions", err);
      } finally {
        setFetchingAll(false);
      }
    }
  };
  const currentPageIds = useMemo(() => questions.data?.content.map((q) => q.id) ?? [], [questions.data]);
  const selectedCurrentPageCount = useMemo(() => currentPageIds.filter((id) => selected.has(id)).length, [currentPageIds, selected]);
  const isAllCurrentPageSelected = currentPageIds.length > 0 && selectedCurrentPageCount === currentPageIds.length;
  const isSomeCurrentPageSelected = selectedCurrentPageCount > 0 && selectedCurrentPageCount < currentPageIds.length;
  const queryParams = useMemo(() => new URLSearchParams(search), [search]);

  return <DataState loading={detail.isLoading} error={detail.error ? getApiErrorMessage(detail.error) : null} empty={!detail.data} onRetry={() => detail.refetch()}>
    {detail.data && <div className="space-y-6">
      <PageHeader title={detail.data.name} description={detail.data.description || "Bộ câu hỏi không có mô tả."} action={<div className="flex gap-2">{detail.data.status === "ARCHIVED" ? <Button variant="secondary" onClick={() => restore.mutate()}><RotateCcw className="h-4 w-4" />Khôi phục</Button> : detail.data.editable && <><Button variant="secondary" onClick={() => setEditing((v) => !v)}>Sửa</Button><Button variant="danger" onClick={() => setConfirmAction("archive")}><Archive className="h-4 w-4" />Lưu trữ</Button></>}</div>} />
      <div className="flex flex-wrap gap-2"><StatusChip>{detail.data.visibility === "PRIVATE" ? "Riêng tư" : "Công khai"}</StatusChip><StatusChip tone={detail.data.editable ? "success" : "neutral"}>{detail.data.editable ? "Có thể chỉnh sửa" : "Chỉ xem"}</StatusChip></div>
      <div className="grid gap-3 sm:grid-cols-4">{[["Tổng số câu", detail.data.stats.questionCount], ["Dễ", detail.data.stats.easy], ["Vừa", detail.data.stats.medium], ["Khó", detail.data.stats.hard]].map(([label, count]) => <div key={label} className="rounded-xl border border-line bg-surface p-5 shadow-soft"><strong className="text-3xl">{count}</strong><small className="block text-muted">{label}</small></div>)}</div>
      {editing && <section className="rounded-xl border border-line bg-surface p-6"><CollectionForm initial={{ name: detail.data.name, description: detail.data.description ?? "", visibility: detail.data.visibility }} loading={update.isPending} onSubmit={(value) => update.mutate(value)} onCancel={() => setEditing(false)} /></section>}
      <div className="flex gap-2 border-b border-line"><button className={`px-4 py-3 text-sm font-semibold ${tab === "inside" ? "border-b-2 border-primary text-primary" : "text-muted"}`} onClick={() => setSearch({ tab: "inside" })}>Câu hỏi trong bộ</button><button className={`px-4 py-3 text-sm font-semibold ${tab === "add" ? "border-b-2 border-primary text-primary" : "text-muted"}`} onClick={() => setSearch({ tab: "add" })}>Thêm câu hỏi</button></div>
      <QuestionFilters value={filter} onChange={(value) => setSearch({ tab, ...Object.fromEntries(Object.entries(value).filter(([, item]) => item)), page: "0" })} />
      <DataState loading={questions.isLoading} error={questions.error ? getApiErrorMessage(questions.error) : null} empty={!questions.data?.content.length} onRetry={() => questions.refetch()}>
        {questions.data && <div className="space-y-4"><QuestionList questions={questions.data.content} selected={selected} onToggle={detail.data.editable ? toggle : undefined} allSelected={isAllCurrentPageSelected} someSelected={isSomeCurrentPageSelected} onToggleAll={detail.data.editable ? handleToggleAll : undefined} disabled={fetchingAll} /><Pagination page={questions.data.page} totalPages={questions.data.totalPages} onChange={(next) => { queryParams.set("page", String(next)); setSearch(queryParams); }} /></div>}
      </DataState>
      {detail.data.editable && selected.size > 0 && <div className="sticky bottom-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-surface p-4 shadow-soft"><strong>Đã chọn {selected.size} câu{fetchingAll && <span className="ml-2 text-xs font-normal text-muted animate-pulse">(Đang tải...)</span>}</strong>{tab === "inside" ? <Button variant="danger" loading={remove.isPending} onClick={() => setConfirmAction("remove")}><Trash2 className="h-4 w-4" />Xóa khỏi bộ</Button> : <Button loading={add.isPending} onClick={() => add.mutate()}><Plus className="h-4 w-4" />Thêm vào bộ</Button>}</div>}
      {detail.data.editable && tab === "add" && <div className="flex justify-end"><Button variant="secondary" loading={addAll.isPending} onClick={() => setConfirmAction("add-all")}>Thêm tất cả kết quả bộ lọc</Button></div>}
      {pendingError && <p role="alert" className="rounded-lg bg-danger/10 p-4 text-danger">{getApiErrorMessage(pendingError)}</p>}
      {detail.data.status === "ARCHIVED" && <p className="rounded-lg bg-warning/10 p-4 text-warning">Bộ câu hỏi đang lưu trữ và chỉ có thể đọc.</p>}
      <ConfirmDialog
        open={confirmAction === "archive"}
        title="Lưu trữ bộ câu hỏi?"
        description={<>Bộ <strong>{detail.data.name}</strong> sẽ chuyển sang chỉ đọc và có thể khôi phục sau.</>}
        confirmLabel="Lưu trữ"
        tone="danger"
        loading={archive.isPending}
        onOpenChange={(open) => !open && setConfirmAction(null)}
        onConfirm={() => archive.mutate()}
      />
      <ConfirmDialog
        open={confirmAction === "add-all"}
        title="Thêm tất cả kết quả?"
        description="Backend sẽ thêm tối đa phạm vi bộ lọc hiện tại, không chỉ các câu đang hiển thị trên trang."
        confirmLabel="Thêm tất cả"
        loading={addAll.isPending}
        onOpenChange={(open) => !open && setConfirmAction(null)}
        onConfirm={() => addAll.mutate()}
      />
      <ConfirmDialog
        open={confirmAction === "remove"}
        title={`Xóa ${selected.size} câu khỏi bộ?`}
        description="Câu hỏi chỉ bị gỡ khỏi bộ này, không bị xóa khỏi ngân hàng câu hỏi."
        confirmLabel="Xóa khỏi bộ"
        tone="danger"
        loading={remove.isPending}
        onOpenChange={(open) => !open && setConfirmAction(null)}
        onConfirm={() => remove.mutate()}
      />
    </div>}
  </DataState>;
}

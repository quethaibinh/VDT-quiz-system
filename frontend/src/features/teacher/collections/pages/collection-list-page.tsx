import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Plus, Search } from "lucide-react";
import { useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { FilterBar } from "@/components/shared/filter-bar";
import { PageHeader } from "@/components/shared/page-header";
import { Pagination } from "@/components/shared/pagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { createCollection, listCollections } from "@/features/teacher/collections/api/collection-api";
import { CollectionCard } from "@/features/teacher/collections/components/collection-card";
import { CollectionForm } from "@/features/teacher/collections/components/collection-form";
import type { CollectionInput } from "@/features/teacher/collections/model/collection-types";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function CollectionListPage() {
  const { subjectId = "" } = useParams();
  const [params, setParams] = useSearchParams();
  const [creating, setCreating] = useState(false);
  const queryClient = useQueryClient();
  const filters = { keyword: params.get("keyword") ?? "", visibility: params.get("visibility") ?? "", ownership: params.get("ownership") ?? "ALL", status: params.get("status") ?? "ACTIVE", page: Number(params.get("page") ?? 0), size: 12, sort: "updatedAt,desc" };
  const query = useQuery({ queryKey: ["teacher", "collections", subjectId, filters], queryFn: () => listCollections(subjectId, filters) });
  const create = useMutation({ mutationFn: (input: CollectionInput) => createCollection(subjectId, input), onSuccess: () => { setCreating(false); queryClient.invalidateQueries({ queryKey: ["teacher", "collections", subjectId] }); } });
  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    next.set("page", "0");
    setParams(next);
  };

  return <div className="space-y-6">
    <PageHeader title="Bộ câu hỏi" description="Tạo và quản lý nguồn câu hỏi cố định cho ca thi." action={<Button onClick={() => setCreating((value) => !value)}><Plus className="h-4 w-4" />Tạo bộ câu hỏi</Button>} />
    {creating && <section className="rounded-xl border border-line bg-surface p-6 shadow-soft"><h2 className="mt-0 text-2xl">Bộ câu hỏi mới</h2><CollectionForm loading={create.isPending} onSubmit={(value) => create.mutate(value)} onCancel={() => setCreating(false)} />{create.error && <p className="text-danger">{getApiErrorMessage(create.error)}</p>}</section>}
    <FilterBar><label className="relative min-w-64 flex-1"><Search className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input className="pl-10" value={filters.keyword} onChange={(e) => update("keyword", e.target.value)} placeholder="Tìm bộ câu hỏi..." /></label><Select value={filters.ownership} onChange={(e) => update("ownership", e.target.value)}><option value="ALL">Tất cả sở hữu</option><option value="MINE">Của tôi</option><option value="SHARED">Được chia sẻ</option></Select><Select value={filters.visibility} onChange={(e) => update("visibility", e.target.value)}><option value="">Tất cả hiển thị</option><option value="PRIVATE">Riêng tư</option><option value="PUBLIC">Công khai</option></Select><Select value={filters.status} onChange={(e) => update("status", e.target.value)}><option value="ACTIVE">Hoạt động</option><option value="ARCHIVED">Lưu trữ</option></Select></FilterBar>
    <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.content.length} onRetry={() => query.refetch()}>
      {query.data && <div className="space-y-4"><div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{query.data.content.map((item) => <CollectionCard key={item.id} collection={item} />)}</div><Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={(page) => update("page", String(page))} /></div>}
    </DataState>
  </div>;
}

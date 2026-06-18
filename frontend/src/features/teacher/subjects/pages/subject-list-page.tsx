import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Input } from "@/components/ui/input";
import { SubjectCard } from "@/features/teacher/subjects/components/subject-card";
import { subjectListQuery } from "@/features/teacher/subjects/model/subject-queries";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function SubjectListPage() {
  const [search, setSearch] = useSearchParams();
  const keyword = search.get("keyword") ?? "";
  const query = useQuery(subjectListQuery(keyword));

  return (
    <div className="space-y-7">
      <PageHeader title="Môn học của tôi" description="Chọn một môn để quản lý ngân hàng câu hỏi và các bộ câu hỏi." />
      <label className="relative block max-w-xl">
        <Search className="absolute left-3 top-3 h-5 w-5 text-muted" />
        <Input aria-label="Tìm môn học" value={keyword} onChange={(event) => setSearch(event.target.value ? { keyword: event.target.value } : {})} className="pl-10" placeholder="Tìm theo mã hoặc tên môn..." />
      </label>
      <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data?.length} emptyMessage="Bạn chưa được phân công môn học nào." onRetry={() => query.refetch()}>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {query.data?.map((subject) => <SubjectCard key={subject.id} subject={subject} />)}
        </div>
      </DataState>
    </div>
  );
}

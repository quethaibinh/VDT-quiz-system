import { useQuery } from "@tanstack/react-query";
import { ClipboardList, Database } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { SubjectWorkspaceAction } from "@/features/teacher/subjects/components/subject-workspace-action";
import { subjectDetailQuery } from "@/features/teacher/subjects/model/subject-queries";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function SubjectDashboardPage() {
  const { subjectId = "" } = useParams();
  const query = useQuery(subjectDetailQuery(subjectId));

  return (
    <DataState loading={query.isLoading} error={query.error ? getApiErrorMessage(query.error) : null} empty={!query.data} onRetry={() => query.refetch()}>
      {query.data && <div className="space-y-7">
        <PageHeader title={query.data.name} description={`${query.data.code} · Không gian quản lý môn học`} action={<Link to="/teacher/subjects"><Button variant="secondary">Đổi môn</Button></Link>} />
        <div className="grid gap-5 lg:grid-cols-2">
          <SubjectWorkspaceAction
            title="Ngân hàng câu hỏi"
            description="Tìm kiếm, lọc và quản lý toàn bộ câu hỏi thuộc môn học này."
            to={`/teacher/subjects/${subjectId}/questions`}
            icon={Database}
            secondaryAction={{ label: "Import câu hỏi từ Excel", to: `/teacher/subjects/${subjectId}/questions/import` }}
          />
          <SubjectWorkspaceAction
            title="Bộ câu hỏi"
            description="Tổ chức câu hỏi thành các bộ dùng làm nguồn tạo đề và ca thi."
            to={`/teacher/subjects/${subjectId}/collections`}
            icon={ClipboardList}
          />
        </div>
      </div>}
    </DataState>
  );
}

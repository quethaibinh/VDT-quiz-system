import { useQuery } from "@tanstack/react-query";
import { ClipboardList, Database, Zap } from "lucide-react";
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
        <PageHeader title={query.data.name} description={`${query.data.code} · Khong gian quan ly mon hoc`} action={<Link to="/teacher/subjects"><Button variant="secondary">Doi mon</Button></Link>} />
        <div className="grid gap-5 lg:grid-cols-2">
          <SubjectWorkspaceAction
            title="Ngan hang cau hoi"
            description="Tim kiem, loc va quan ly toan bo cau hoi thuoc mon hoc nay."
            to={`/teacher/subjects/${subjectId}/questions`}
            icon={Database}
            secondaryAction={{ label: "Import cau hoi tu Excel", to: `/teacher/subjects/${subjectId}/questions/import` }}
          />
          <SubjectWorkspaceAction
            title="Bo cau hoi"
            description="To chuc cau hoi thanh cac bo dung lam nguon tao de, ca thi va quiz."
            to={`/teacher/subjects/${subjectId}/collections`}
            icon={ClipboardList}
          />
          <SubjectWorkspaceAction
            title="Quiz"
            description="Tao quiz truc tiep tu bo cau hoi, chuan bi ma phong va mo phong khi lop san sang."
            to={`/teacher/subjects/${subjectId}/live-quizzes`}
            icon={Zap}
          />
        </div>
      </div>}
    </DataState>
  );
}

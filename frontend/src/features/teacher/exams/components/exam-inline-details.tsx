import { useQuery } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import {
  examKeys,
  getExam,
} from "@/features/teacher/exams/api/exam-repository";
import type {
  ExamDetail,
  HandleViolation,
  ShowResultPolicy,
} from "@/features/teacher/exams/model/exam-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

const resultPolicyLabels: Record<ShowResultPolicy, string> = {
  NEVER: "Không hiển thị",
  AFTER_SUBMIT: "Sau khi nộp bài",
  AFTER_CLOSED: "Sau khi ca thi kết thúc",
};

const violationLabels: Record<HandleViolation, string> = {
  LOCK: "Khóa bài thi",
  PAUSE: "Tạm dừng bài thi",
  WARN: "Cảnh báo",
};

export function ExamInlineDetails({
  subjectId,
  examId,
  panelId,
  labelledBy,
}: {
  subjectId: string;
  examId: string;
  panelId: string;
  labelledBy: string;
}) {
  const detail = useQuery({
    queryKey: examKeys.detail(subjectId, examId),
    queryFn: () => getExam(subjectId, examId),
  });

  return (
    <div id={panelId} role="region" aria-labelledby={labelledBy} className="border-t border-line px-5 py-5">
      {detail.isLoading && <p role="status" className="m-0 text-sm text-muted">Đang tải chi tiết ca thi...</p>}
      {detail.error && (
        <div role="alert" className="rounded-lg bg-danger/10 p-4 text-sm text-danger">
          <p className="mt-0">{getApiErrorMessage(detail.error)}</p>
          <Button variant="secondary" onClick={() => detail.refetch()}>Thử lại</Button>
        </div>
      )}
      {detail.data && <DetailContent exam={detail.data} />}
    </div>
  );
}

function DetailContent({ exam }: { exam: ExamDetail }) {
  const totalQuestions = exam.easyCount + exam.mediumCount + exam.hardCount;
  return (
    <div className="space-y-5">
      <section>
        <h3 className="mb-3 mt-0 text-lg">Thông tin chung</h3>
        <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Detail label="Mã ca thi" value={exam.code} />
          <Detail label="Bộ câu hỏi" value={exam.collectionName} />
          <Detail label="Số học sinh" value={`${exam.assignedCount}`} />
          <Detail label="Tổng số câu" value={`${totalQuestions}`} />
        </dl>
        <p className="mb-0 mt-3 text-sm text-muted">
          {exam.description?.trim() || "Ca thi không có mô tả."}
        </p>
      </section>

      <section>
        <h3 className="mb-3 mt-0 text-lg">Cấu trúc và thời gian</h3>
        <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Detail label="Mức độ câu hỏi" value={`${exam.easyCount} dễ · ${exam.mediumCount} trung bình · ${exam.hardCount} khó`} />
          <Detail label="Bắt đầu" value={formatDateTime(exam.startAt)} />
          <Detail label="Kết thúc" value={formatDateTime(exam.endAt)} />
          <Detail label="Thời lượng" value={`${exam.durationMinutes} phút`} />
          <Detail label="Mở trước giờ thi" value={`${exam.joinBeforeMinutes} phút`} />
          <Detail label="Cho vào trễ" value={`${exam.joinAfterMinutes} phút`} />
        </dl>
      </section>

      <section>
        <h3 className="mb-3 mt-0 text-lg">Chính sách làm bài</h3>
        <dl className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Detail label="Trộn câu hỏi" value={yesNo(exam.shuffleQuestions)} />
          <Detail label="Trộn đáp án" value={yesNo(exam.shuffleOptions)} />
          <Detail label="Hiển thị kết quả" value={resultPolicyLabels[exam.showResultPolicy]} />
          <Detail label="Tự động nộp bài" value={yesNo(exam.autoSubmit)} />
          <Detail label="Yêu cầu toàn màn hình" value={yesNo(exam.requireFullscreen)} />
          <Detail label="Số vi phạm tối đa" value={`${exam.maxViolationAllowed}`} />
          <Detail label="Xử lý vi phạm" value={violationLabels[exam.handleViolation]} />
        </dl>
      </section>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-canvas p-3">
      <dt className="text-xs font-semibold uppercase tracking-wide text-muted">{label}</dt>
      <dd className="mb-0 ml-0 mt-1 text-sm font-medium text-ink">{value}</dd>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("vi-VN");
}

function yesNo(value: boolean) {
  return value ? "Có" : "Không";
}

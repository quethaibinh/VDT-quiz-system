import type { ExamSummary } from "@/features/teacher/exams/model/exam-contracts";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function getScheduleDisabledReason(
  exam: ExamSummary,
  now = Date.now(),
): string | null {
  if (exam.assignedCount < 1) {
    return "Cần phân công ít nhất 1 học sinh.";
  }

  if (new Date(exam.startAt).getTime() <= now) {
    return "Thời gian bắt đầu phải ở tương lai.";
  }

  return null;
}

const scheduleErrorMessages: Record<string, string> = {
  EXAM_NOT_FOUND: "Không tìm thấy ca thi hoặc bạn không còn quyền truy cập.",
  EXAM_ASSIGNMENT_REQUIRED: "Cần phân công ít nhất 1 học sinh trước khi lên lịch.",
  EXAM_START_MUST_BE_IN_FUTURE: "Thời gian bắt đầu phải ở tương lai.",
  EXAM_NOT_EDITABLE: "Ca thi không còn ở trạng thái bản nháp.",
  EXAM_CHANGED_DURING_SCHEDULING: "Ca thi vừa được thay đổi. Dữ liệu đã được làm mới, vui lòng kiểm tra lại.",
  EXAM_BLUEPRINT_CHANGED: "Cấu hình ca thi vừa thay đổi. Vui lòng kiểm tra lại trước khi lên lịch.",
  EXAM_SNAPSHOT_ALREADY_EXISTS: "Ca thi đã có dữ liệu đề thi và không thể lên lịch lại.",
  COLLECTION_NOT_FOUND: "Không tìm thấy bộ câu hỏi đã chọn.",
  INVALID_COLLECTION_SELECTION: "Bộ câu hỏi không còn hợp lệ hoặc không thể sử dụng.",
  INVALID_COLLECTION_SNAPSHOT: "Không thể đọc dữ liệu bộ câu hỏi. Vui lòng kiểm tra lại.",
  INSUFFICIENT_COLLECTION_QUOTA: "Bộ câu hỏi không đủ số lượng theo mức độ đã cấu hình.",
  DUPLICATE_SNAPSHOT_QUESTION: "Bộ câu hỏi chứa dữ liệu trùng lặp và chưa thể lên lịch.",
  QUESTION_SERVICE_UNAVAILABLE: "Dịch vụ câu hỏi đang tạm thời gián đoạn. Vui lòng thử lại sau.",
  INVALID_QUESTION_SERVICE_RESPONSE: "Dữ liệu bộ câu hỏi không hợp lệ. Vui lòng kiểm tra lại hoặc thử sau.",
  CONCURRENT_OR_DUPLICATE_UPDATE: "Ca thi vừa được cập nhật ở nơi khác. Dữ liệu đã được làm mới.",
  SNAPSHOT_SERIALIZATION_FAILED: "Không thể chuẩn bị dữ liệu đề thi. Vui lòng thử lại sau.",
};

export function getScheduleErrorMessage(error: unknown) {
  const message = getApiErrorMessage(error);
  return scheduleErrorMessages[message] ?? message;
}

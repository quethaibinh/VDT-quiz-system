import { AlertCircle, Lock, ShieldAlert, type LucideIcon } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

const ERROR_MESSAGES: Record<string, { title: string; desc: string; icon: LucideIcon }> = {
  EXAM_RUNTIME_NOT_READY: {
    title: "Ca thi chưa sẵn sàng",
    desc: "Hệ thống đang khởi tạo ca thi. Vui lòng chờ trong giây lát và thử lại.",
    icon: AlertCircle,
  },
  STUDENT_NOT_ASSIGNED: {
    title: "Không có quyền tham gia",
    desc: "Tài khoản của bạn chưa được phân công vào ca thi này. Vui lòng liên hệ giáo viên.",
    icon: ShieldAlert,
  },
  EXAM_JOIN_NOT_OPEN: {
    title: "Chưa mở phòng chờ",
    desc: "Ca thi chưa đến giờ mở cửa vào thi.",
    icon: AlertCircle,
  },
  EXAM_LATE_JOIN_CLOSED: {
    title: "Đã quá hạn vào thi",
    desc: "Cửa sổ vào thi muộn đã đóng. Bạn không thể tham gia làm bài nữa.",
    icon: ShieldAlert,
  },
  EXAM_ALREADY_ENDED: {
    title: "Ca thi đã kết thúc",
    desc: "Thời gian làm bài đã hết. Ca thi đã khóa.",
    icon: AlertCircle,
  },
  EXAM_ALREADY_LOCKED: {
    title: "Phiên thi đã bị khóa",
    desc: "Phiên thi của bạn đã bị khóa do vi phạm quy chế hoặc can thiệp của giám thị.",
    icon: Lock,
  },
  SESSION_NOT_FOUND: {
    title: "Không tìm thấy phiên thi",
    desc: "Không tìm thấy lịch sử làm bài của bạn. Vui lòng thử lại hoặc vào từ danh sách ca thi.",
    icon: AlertCircle,
  },
  UNAUTHORIZED_SESSION: {
    title: "Truy cập bị từ chối",
    desc: "Phiên làm bài này không thuộc về bạn. Vui lòng đăng nhập lại.",
    icon: ShieldAlert,
  },
};

interface ExamLobbyStateProps {
  errorCode: string;
}

// Hien thi giao dien thong bao loi nghiep vu khi vao phong cho
export function ExamLobbyState({ errorCode }: ExamLobbyStateProps) {
  const errorInfo = ERROR_MESSAGES[errorCode] ?? {
    title: "Đã xảy ra lỗi",
    desc: `Lỗi hệ thống: ${errorCode}. Vui lòng liên hệ hỗ trợ kỹ thuật.`,
    icon: AlertCircle,
  };
  const Icon = errorInfo.icon;

  return (
    <div className="mx-auto my-12 flex max-w-lg flex-col items-center justify-center space-y-6 rounded-2xl border border-line bg-surface p-8 text-center shadow-soft">
      <div className="rounded-full bg-danger/10 p-4 text-danger">
        <Icon size={48} />
      </div>
      <div className="space-y-2">
        <h2 className="text-xl font-bold text-ink">{errorInfo.title}</h2>
        <p className="text-sm leading-relaxed text-muted">{errorInfo.desc}</p>
      </div>
      <Link to="/student/exams">
        <Button variant="secondary">Quay lại danh sách ca thi</Button>
      </Link>
    </div>
  );
}

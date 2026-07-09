import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";

export function ForbiddenPage() {
  return <main className="grid min-h-screen place-items-center bg-canvas p-6"><div className="text-center"><h1 className="text-6xl">Không có quyền truy cập</h1><p className="text-muted">Tài khoản hiện tại không thuộc không gian giáo viên.</p><Link to="/login"><Button>Quay lại đăng nhập</Button></Link></div></main>;
}

export function NotFoundPage() {
  return <main className="grid min-h-screen place-items-center bg-canvas p-6"><div className="text-center"><h1 className="text-6xl">Không tìm thấy trang</h1><Link to="/teacher/subjects"><Button>Về môn học</Button></Link></div></main>;
}

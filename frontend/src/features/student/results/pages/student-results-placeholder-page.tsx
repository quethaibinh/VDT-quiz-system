import { AlertCircle } from "lucide-react";

// Trang placeholder ket qua thi cua hoc sinh (hien tai chua duoc ho tro)
export function StudentResultsPlaceholderPage() {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center min-h-[50vh]">
      <div className="bg-warning/10 p-4 rounded-full text-warning mb-4">
        <AlertCircle size={48} />
      </div>
      <h1 className="text-2xl font-bold text-ink mb-2">Xem kết quả thi</h1>
      <p className="text-muted max-w-md">
        Tính năng xem kết quả thi chi tiết của học sinh hiện tại chưa được hỗ trợ trong phiên bản này.
      </p>
    </div>
  );
}

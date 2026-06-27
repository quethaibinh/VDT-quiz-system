import { CheckCircle2, CloudLightning, Loader2 } from "lucide-react";

export type SaveState = "idle" | "saving" | "error";

interface SaveIndicatorProps {
  state: SaveState;
  lastSavedAt?: string | null;
}

// Hien thi trang thai dong bo hoa dap an voi backend
export function SaveIndicator({ state, lastSavedAt }: SaveIndicatorProps) {
  return (
    <div className="flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold">
      {state === "saving" && (
        <span className="flex items-center gap-1 text-primary">
          <Loader2 className="animate-spin" size={14} />
          <span>Đang lưu...</span>
        </span>
      )}
      {state === "error" && (
        <span className="flex items-center gap-1 rounded bg-danger/10 px-2 py-0.5 text-danger">
          <CloudLightning size={14} />
          <span>Lỗi kết nối - đang giữ dữ liệu trên máy</span>
        </span>
      )}
      {state === "idle" && (
        <span className="flex items-center gap-1 text-success">
          <CheckCircle2 size={14} />
          <span>
            {lastSavedAt
              ? `Đã lưu: ${new Date(lastSavedAt).toLocaleTimeString("vi-VN")}`
              : "Chưa có thay đổi mới"}
          </span>
        </span>
      )}
    </div>
  );
}

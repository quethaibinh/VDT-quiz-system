import { Button } from "@/components/ui/button";

// Hien thi nut nop bai chua duoc ho tro trong phien ban nay
export function UnsupportedSubmitButton() {
  return (
    <div className="group relative">
      <Button variant="primary" disabled className="bg-primary/50 text-white/70">
        Nộp bài
      </Button>
      <div className="absolute right-0 top-full z-10 mt-2 hidden whitespace-nowrap rounded bg-ink p-2 text-xs text-white shadow-md group-hover:block">
        Tính năng nộp bài tự động/thủ công chưa được hỗ trợ.
      </div>
    </div>
  );
}

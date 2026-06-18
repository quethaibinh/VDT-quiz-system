import type { ReactNode } from "react";
import { AlertCircle, Inbox } from "lucide-react";
import { Button } from "@/components/ui/button";

export function DataState({ loading, error, empty, emptyMessage = "Chưa có dữ liệu phù hợp.", onRetry, children }: { loading?: boolean; error?: string | null; empty?: boolean; emptyMessage?: string; onRetry?: () => void; children: ReactNode }) {
  if (loading) return <div className="rounded-xl border border-line bg-surface p-8 text-center text-muted shadow-soft">Đang tải dữ liệu...</div>;
  if (error) return <div className="rounded-xl border border-danger/30 bg-surface p-8 text-center"><AlertCircle className="mx-auto mb-3 text-danger" /><p>{error}</p>{onRetry && <Button variant="secondary" onClick={onRetry}>Thử lại</Button>}</div>;
  if (empty) return <div className="rounded-xl border border-line bg-surface p-8 text-center text-muted"><Inbox className="mx-auto mb-3" /><p>{emptyMessage}</p></div>;
  return children;
}

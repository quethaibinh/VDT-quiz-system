import { Button } from "@/components/ui/button";

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="flex items-center justify-end gap-2" aria-label="Phân trang">
      <Button variant="secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>Trước</Button>
      <span className="px-2 text-sm text-muted">Trang {page + 1}/{totalPages}</span>
      <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Sau</Button>
    </nav>
  );
}

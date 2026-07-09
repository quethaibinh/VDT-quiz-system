import { Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { MonitorFilter } from "@/features/teacher/monitoring/model/monitor-state";

const filterOptions: { value: MonitorFilter; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "WARNING", label: "Cảnh báo" },
  { value: "NOT_JOIN", label: "Chưa tham gia" },
  { value: "OFFLINE", label: "Mất kết nối" },
  { value: "SUBMITTED", label: "Đã nộp" },
  { value: "LOCKED", label: "Bị khóa" },
];

export function MonitorFilters({
  filter,
  keyword,
  onFilterChange,
  onKeywordChange,
}: {
  filter: MonitorFilter;
  keyword: string;
  onFilterChange: (filter: MonitorFilter) => void;
  onKeywordChange: (keyword: string) => void;
}) {
  return (
    <section className="space-y-3">
      <div className="flex gap-2 overflow-x-auto pb-1">
        {filterOptions.map((option) => (
          <Button
            key={option.value}
            type="button"
            variant={filter === option.value ? "primary" : "secondary"}
            className="shrink-0 px-3"
            onClick={() => onFilterChange(option.value)}
          >
            {option.label}
          </Button>
        ))}
      </div>
      <label className="relative block">
        <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted" size={16} />
        <Input
          className="pl-9"
          value={keyword}
          onChange={(event) => onKeywordChange(event.target.value)}
          placeholder="Tìm học sinh..."
          aria-label="Tìm học sinh"
        />
      </label>
    </section>
  );
}

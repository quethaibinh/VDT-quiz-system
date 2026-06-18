import { Search } from "lucide-react";
import { FilterBar } from "@/components/shared/filter-bar";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";

export interface QuestionFilterValue {
  keyword: string;
  difficulty: string;
  visibility: string;
  ownerScope: string;
  questionType: string;
}

export function QuestionFilters({ value, onChange }: { value: QuestionFilterValue; onChange: (next: QuestionFilterValue) => void }) {
  const set = (key: keyof QuestionFilterValue, next: string) => onChange({ ...value, [key]: next });
  return (
    <FilterBar>
      <label className="relative min-w-64 flex-1"><Search className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input aria-label="Tìm nội dung câu hỏi" className="pl-10" placeholder="Tìm nội dung câu hỏi..." value={value.keyword} onChange={(e) => set("keyword", e.target.value)} /></label>
      <Select aria-label="Độ khó" value={value.difficulty} onChange={(e) => set("difficulty", e.target.value)}><option value="">Tất cả độ khó</option><option value="EASY">Dễ</option><option value="MEDIUM">Vừa</option><option value="HARD">Khó</option></Select>
      <Select aria-label="Hiển thị" value={value.visibility} onChange={(e) => set("visibility", e.target.value)}><option value="">Tất cả hiển thị</option><option value="PUBLIC">Công khai</option><option value="PRIVATE">Riêng tư</option></Select>
      <Select aria-label="Sở hữu" value={value.ownerScope} onChange={(e) => set("ownerScope", e.target.value)}><option value="ALL">Tất cả sở hữu</option><option value="MINE">Của tôi</option><option value="SHARED">Được chia sẻ</option></Select>
      <Select aria-label="Loại câu hỏi" value={value.questionType} onChange={(e) => set("questionType", e.target.value)}><option value="">Tất cả loại</option><option value="SINGLE_CHOICE">Một đáp án</option><option value="MULTI_CHOICE">Nhiều đáp án</option></Select>
    </FilterBar>
  );
}

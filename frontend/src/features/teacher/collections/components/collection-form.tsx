import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import type { CollectionInput } from "@/features/teacher/collections/model/collection-types";

const schema = z.object({
  name: z.string().min(1, "Tên bộ câu hỏi là bắt buộc."),
  description: z.string().optional(),
  visibility: z.enum(["PRIVATE", "PUBLIC"]),
});

export function CollectionForm({ initial, loading, onSubmit, onCancel }: { initial?: CollectionInput; loading?: boolean; onSubmit: (value: CollectionInput) => void; onCancel?: () => void }) {
  const { register, handleSubmit, formState: { errors } } = useForm<CollectionInput>({
    resolver: zodResolver(schema),
    defaultValues: initial ?? { name: "", description: "", visibility: "PRIVATE" },
  });
  return <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
    <label className="block text-sm font-semibold">Tên bộ câu hỏi<Input className="mt-2" {...register("name")} /></label>
    {errors.name && <p className="text-sm text-danger">{errors.name.message}</p>}
    <label className="block text-sm font-semibold">Mô tả<textarea className="mt-2 min-h-24 w-full rounded-lg border border-line bg-surface p-3" {...register("description")} /></label>
    <label className="block text-sm font-semibold">Chế độ hiển thị<Select className="mt-2 w-full" {...register("visibility")}><option value="PRIVATE">Riêng tư</option><option value="PUBLIC">Công khai</option></Select></label>
    <div className="flex justify-end gap-2">{onCancel && <Button type="button" variant="secondary" onClick={onCancel}>Hủy</Button>}<Button type="submit" loading={loading}>Lưu bộ câu hỏi</Button></div>
  </form>;
}

import * as Dialog from "@radix-ui/react-dialog";
import type { ReactNode } from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  loading,
  tone = "primary",
  onConfirm,
  onOpenChange,
}: {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  loading?: boolean;
  tone?: "primary" | "danger";
  onConfirm: () => void;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-ink/35" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[min(92vw,480px)] -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-line bg-surface p-6 shadow-soft">
          <Dialog.Title className="m-0 text-3xl">{title}</Dialog.Title>
          <Dialog.Description className="mt-3 text-sm leading-6 text-muted">{description}</Dialog.Description>
          <Dialog.Close className="absolute right-4 top-4 rounded-lg p-2" aria-label="Đóng"><X className="h-5 w-5" /></Dialog.Close>
          <div className="mt-6 flex justify-end gap-2">
            <Dialog.Close asChild><Button variant="secondary">Hủy</Button></Dialog.Close>
            <Button variant={tone} loading={loading} onClick={onConfirm}>{confirmLabel}</Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

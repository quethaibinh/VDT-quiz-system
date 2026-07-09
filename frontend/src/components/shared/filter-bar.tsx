import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export function FilterBar({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn("flex flex-wrap items-end gap-3 rounded-xl border border-line bg-surface p-4 shadow-soft", className)}>{children}</div>;
}

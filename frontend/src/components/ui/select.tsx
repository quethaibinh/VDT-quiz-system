import type { SelectHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

export function Select({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cn("min-h-11 rounded-lg border border-line bg-surface px-3 text-sm text-ink focus:border-primary", className)} {...props} />;
}

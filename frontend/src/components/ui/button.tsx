import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "danger";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
}

export function Button({ className, variant = "primary", loading, children, disabled, ...props }: ButtonProps) {
  const styles: Record<Variant, string> = {
    primary: "bg-primary text-white hover:bg-primary-hover",
    secondary: "border border-line bg-surface text-ink hover:border-primary",
    ghost: "bg-transparent text-ink hover:bg-primary/5",
    danger: "border border-danger text-danger hover:bg-danger/5",
  };
  return (
    <button
      className={cn("inline-flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50", styles[variant], className)}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? "Đang xử lý..." : children}
    </button>
  );
}

import type { ReactNode } from "react";

export function PageHeader({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <header className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 className="m-0 text-4xl font-semibold leading-tight md:text-5xl">{title}</h1>
        {description && <p className="mt-2 text-sm text-muted md:text-base">{description}</p>}
      </div>
      {action}
    </header>
  );
}

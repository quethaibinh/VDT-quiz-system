import { ArrowRight, type LucideIcon } from "lucide-react";
import { Link } from "react-router-dom";

interface SubjectWorkspaceActionProps {
  title: string;
  description: string;
  to: string;
  icon: LucideIcon;
  secondaryAction?: { label: string; to: string };
}

export function SubjectWorkspaceAction({ title, description, to, icon: Icon, secondaryAction }: SubjectWorkspaceActionProps) {
  return (
    <article className="flex h-full flex-col rounded-xl border border-line bg-surface p-6 shadow-soft">
      <div className="grid h-12 w-12 place-items-center rounded-xl bg-primary/10 text-primary"><Icon /></div>
      <h2 className="mb-2 mt-6 text-2xl">{title}</h2>
      <p className="mt-0 flex-1 text-sm leading-6 text-muted">{description}</p>
      <div className="mt-6 flex flex-wrap items-center gap-4">
        <Link to={to} className="inline-flex items-center gap-2 font-semibold text-primary">
          Mở {title.toLowerCase()} <ArrowRight className="h-4 w-4" />
        </Link>
        {secondaryAction && <Link to={secondaryAction.to} className="text-sm font-semibold text-muted hover:text-primary">{secondaryAction.label}</Link>}
      </div>
    </article>
  );
}

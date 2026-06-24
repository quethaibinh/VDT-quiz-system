import { ChevronDown, LogOut, Menu, X, type LucideIcon } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-context";
import { cn } from "@/lib/cn";

export interface WorkspaceNavigationItem {
  label: string;
  icon: LucideIcon;
  path: string;
  active: (pathname: string) => boolean;
}

export function WorkspaceShell({ homePath, workspaceLabel, roleLabel, navigation, children }: {
  homePath: string;
  workspaceLabel: string;
  roleLabel: string;
  navigation: WorkspaceNavigationItem[];
  children: ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const { session, signOut } = useAuth();
  const { pathname } = useLocation();
  const name = session?.claims.fullName || session?.claims.displayName || session?.claims.username || roleLabel;
  const sidebar = (
    <aside className="flex h-full w-72 flex-col border-r border-line bg-surface p-5">
      <div className="flex items-center justify-between">
        <Link to={homePath} onClick={() => setOpen(false)} className="font-serif text-xl font-bold tracking-[0.12em] text-primary">SAHARA QUIZ</Link>
        <button className="rounded-lg p-1 lg:hidden" aria-label="Đóng menu" onClick={() => setOpen(false)}><X /></button>
      </div>
      <nav className="mt-10 space-y-2" aria-label={workspaceLabel}>
        {navigation.map(({ label, icon: Icon, path, active }) => {
          const selected = active(pathname);
          return <Link key={path} to={path} aria-current={selected ? "page" : undefined} onClick={() => setOpen(false)} className={cn("flex items-center gap-3 rounded-lg px-3 py-3 text-sm font-semibold text-muted hover:bg-primary/5 hover:text-primary", selected && "bg-primary/10 text-primary")}><Icon className="h-5 w-5" />{label}</Link>;
        })}
      </nav>
      <Button variant="ghost" className="mt-auto justify-start" onClick={signOut}><LogOut className="h-4 w-4" />Đăng xuất</Button>
    </aside>
  );
  return (
    <div className="min-h-screen bg-canvas lg:grid lg:grid-cols-[288px_1fr]">
      <div className="hidden lg:block">{sidebar}</div>
      {open && <div className="fixed inset-0 z-40 bg-ink/30 lg:hidden" onClick={() => setOpen(false)}><div className="h-full w-72" onClick={(event) => event.stopPropagation()}>{sidebar}</div></div>}
      <div className="min-w-0">
        <header className="sticky top-0 z-30 flex h-18 items-center justify-between border-b border-line bg-canvas/95 px-4 backdrop-blur md:px-8">
          <button className="rounded-lg p-1 lg:hidden" aria-label="Mở menu" onClick={() => setOpen(true)}><Menu /></button>
          <span className="hidden text-sm text-muted lg:block">{workspaceLabel}</span>
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-full bg-primary/10 font-semibold text-primary">{name.slice(0, 2).toUpperCase()}</div>
            <div className="hidden sm:block"><p className="m-0 text-sm font-semibold">{name}</p><p className="m-0 text-xs text-muted">{roleLabel}</p></div>
            <ChevronDown className="h-4 w-4 text-muted" />
          </div>
        </header>
        <main className="mx-auto max-w-[1440px] p-4 md:p-8">{children}</main>
      </div>
    </div>
  );
}

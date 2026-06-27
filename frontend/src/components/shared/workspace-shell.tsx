import { ChevronDown, LogOut, Menu, X, ChevronLeft, ChevronRight, type LucideIcon } from "lucide-react";
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

  // Khoi tao trang thai thu gon tu localStorage, mac dinh la mo rong (false)
  const [isCollapsed, setIsCollapsed] = useState(() => {
    try {
      return localStorage.getItem("sahara_sidebar_collapsed") === "true";
    } catch {
      return false;
    }
  });

  const toggleCollapse = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem("sahara_sidebar_collapsed", String(next));
      } catch {
        // Bo qua neu loi localStorage
      }
      return next;
    });
  };

  const renderSidebar = (collapsed: boolean) => (
    <aside className={cn("flex h-full flex-col border-r border-line bg-surface p-5 transition-all duration-300", collapsed ? "w-18 items-center px-2" : "w-72")}>
      <div className="flex items-center justify-between w-full">
        <Link to={homePath} onClick={() => setOpen(false)} className={cn("font-serif text-xl font-bold tracking-[0.12em] text-primary truncate", collapsed && "text-center w-full text-base")}>
          {collapsed ? "SQ" : "SAHARA QUIZ"}
        </Link>
        {!collapsed && (
          <button className="rounded-lg p-1 lg:hidden" aria-label="Đóng menu" onClick={() => setOpen(false)}>
            <X />
          </button>
        )}
      </div>
      <nav className={cn("mt-10 space-y-2 w-full", collapsed && "flex flex-col items-center")} aria-label={workspaceLabel}>
        {navigation.map(({ label, icon: Icon, path, active }) => {
          const selected = active(pathname);
          return (
            <Link
              key={path}
              to={path}
              title={collapsed ? label : undefined}
              aria-current={selected ? "page" : undefined}
              onClick={() => setOpen(false)}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-3 text-sm font-semibold text-muted hover:bg-primary/5 hover:text-primary transition-all duration-200 w-full",
                selected && "bg-primary/10 text-primary",
                collapsed && "justify-center px-0 w-10 h-10"
              )}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              {!collapsed && <span>{label}</span>}
            </Link>
          );
        })}
      </nav>
      {/* Nut toggle thu gon sidebar chi hien thi tren desktop (lg) */}
      <Button
        variant="ghost"
        className={cn("mt-auto justify-start w-full hidden lg:flex", collapsed && "justify-center px-0 w-10 h-10 mb-2")}
        onClick={toggleCollapse}
        title={collapsed ? "Mở rộng" : "Thu gọn"}
      >
        {collapsed ? (
          <ChevronRight className="h-4 w-4" />
        ) : (
          <>
            <ChevronLeft className="h-4 w-4" />
            <span className="ml-2">Thu gọn</span>
          </>
        )}
      </Button>
      <Button
        variant="ghost"
        title={collapsed ? "Đăng xuất" : undefined}
        className={cn("justify-start w-full", collapsed ? "justify-center px-0 w-10 h-10 mt-0" : "mt-2")}
        onClick={signOut}
      >
        <LogOut className="h-4 w-4 flex-shrink-0" />
        {!collapsed && <span className="ml-2">Đăng xuất</span>}
      </Button>
    </aside>
  );

  return (
    <div className={cn("min-h-screen bg-canvas lg:grid transition-all duration-300", isCollapsed ? "lg:grid-cols-[72px_1fr]" : "lg:grid-cols-[288px_1fr]")}>
      <div className="hidden lg:block sticky top-0 h-screen">{renderSidebar(isCollapsed)}</div>
      {open && (
        <div className="fixed inset-0 z-40 bg-ink/30 lg:hidden" onClick={() => setOpen(false)}>
          <div className="h-full w-72" onClick={(event) => event.stopPropagation()}>
            {renderSidebar(false)}
          </div>
        </div>
      )}
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


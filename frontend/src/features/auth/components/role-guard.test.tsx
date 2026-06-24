import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RoleGuard } from "@/features/auth/components/role-guard";

const useAuth = vi.fn();
vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => useAuth() }));

function LocationProbe() {
  const location = useLocation();
  return <span>{location.pathname}{location.search}</span>;
}

describe("RoleGuard", () => {
  beforeEach(() => useAuth.mockReset());

  it("preserves pathname and search for unauthenticated users", () => {
    useAuth.mockReturnValue({ session: null });
    renderRoutes("/admin/users?role=TEACHER&page=2");
    expect(screen.getByText("/login?returnTo=%2Fadmin%2Fusers%3Frole%3DTEACHER%26page%3D2")).toBeInTheDocument();
  });

  it("rejects a cross-role session", () => {
    useAuth.mockReturnValue({ session: { claims: { userRole: "TEACHER" } } });
    renderRoutes("/admin/users");
    expect(screen.getByText("/forbidden")).toBeInTheDocument();
  });

  it("renders the protected route for the required role", () => {
    useAuth.mockReturnValue({ session: { claims: { userRole: "ADMIN" } } });
    renderRoutes("/admin/users");
    expect(screen.getByText("protected")).toBeInTheDocument();
  });
});

function renderRoutes(entry: string) {
  return render(<MemoryRouter initialEntries={[entry]}><Routes><Route element={<RoleGuard role="ADMIN" />}><Route path="/admin/users" element={<span>protected</span>} /></Route><Route path="*" element={<LocationProbe />} /></Routes></MemoryRouter>);
}

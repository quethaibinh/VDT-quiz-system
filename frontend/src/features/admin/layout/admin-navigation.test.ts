import { describe, expect, it } from "vitest";
import { adminNavigationItems } from "@/features/admin/layout/admin-navigation";
import { getRoleHome } from "@/features/auth/lib/role-home";

describe("Admin navigation", () => {
  it("maps supported roles to isolated workspaces", () => {
    expect(getRoleHome("ADMIN")).toBe("/admin/dashboard");
    expect(getRoleHome("TEACHER")).toBe("/teacher/subjects");
    expect(getRoleHome("STUDENT")).toBe("/student/dashboard");
  });

  it("keeps import separate from the user detail active state", () => {
    const users = adminNavigationItems.find((item) => item.path === "/admin/users");
    const imports = adminNavigationItems.find((item) => item.path === "/admin/users/import");
    expect(users?.active("/admin/users/abc")).toBe(true);
    expect(users?.active("/admin/users/import")).toBe(false);
    expect(imports?.active("/admin/users/import")).toBe(true);
  });
});

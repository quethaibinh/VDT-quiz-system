import { beforeEach, describe, expect, it, vi } from "vitest";
import { importAdminUsers } from "@/features/admin/imports/api/admin-user-import-api";
import { assignSubjectTeacher } from "@/features/admin/assignments/api/assignment-api";
import { listAdminUsers, updateAdminUserStatus } from "@/features/admin/users/api/admin-user-api";
import { apiClient } from "@/lib/http/api-client";

vi.mock("@/lib/http/api-client", () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const response = <T>(data: T) => ({ data: { timestamp: "", status: 200, message: "OK", data } });

describe("Admin real API adapters", () => {
  beforeEach(() => vi.clearAllMocks());

  it("sends URL-driven user filters to the Admin Auth endpoint", async () => {
    vi.mocked(apiClient.get).mockResolvedValue(response({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0, first: false, last: true }));
    await listAdminUsers({ keyword: "an", userType: "TEACHER", status: "ACTIVE", page: 1, size: 20, sort: "fullName,asc" });
    expect(apiClient.get).toHaveBeenCalledWith("/v1/api/admin/auth-service/users", { params: { keyword: "an", userType: "TEACHER", status: "ACTIVE", page: 1, size: 20, sort: "fullName,asc" } });
  });

  it("uses PATCH for server-authoritative status changes", async () => {
    vi.mocked(apiClient.patch).mockResolvedValue(response({ id: "u1", status: "INACTIVE" }));
    await updateAdminUserStatus("u1", "INACTIVE");
    expect(apiClient.patch).toHaveBeenCalledWith("/v1/api/admin/auth-service/users/u1/status", { status: "INACTIVE" });
  });

  it("uploads the Excel file with multipart field file", async () => {
    vi.mocked(apiClient.post).mockResolvedValue(response({ totalRows: 0, successCount: 0, failedCount: 0, errors: [] }));
    const file = new File(["sheet"], "users.xlsx");
    await importAdminUsers(file);
    const form = vi.mocked(apiClient.post).mock.calls[0][1] as FormData;
    expect(form.get("file")).toBe(file);
    expect(apiClient.post).toHaveBeenCalledWith("/v1/api/admin/auth-service/users/import", form, { timeout: 60_000 });
  });

  it("assigns only the selected real teacher UUID", async () => {
    vi.mocked(apiClient.post).mockResolvedValue(response({ teacherId: "teacher-real-id" }));
    await assignSubjectTeacher("subject-1", "teacher-real-id");
    expect(apiClient.post).toHaveBeenCalledWith("/v1/api/admin/question-service/subjects/subject-1/teachers", { teacherId: "teacher-real-id" });
  });
});

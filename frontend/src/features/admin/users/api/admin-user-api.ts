import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { AdminUserDetail, AdminUserFilters, AdminUserPage, AdminUserStatistics, UpdateAdminUserInput, UserStatus } from "@/features/admin/users/model/admin-user-contracts";

const base = "/v1/api/admin/auth-service/users";

export async function listAdminUsers(params: AdminUserFilters) {
  const response = await apiClient.get<ApiResponse<AdminUserPage>>(base, { params });
  return unwrap(response.data);
}

export async function getAdminUser(userId: string) {
  const response = await apiClient.get<ApiResponse<AdminUserDetail>>(`${base}/${userId}`);
  return unwrap(response.data);
}

export async function getAdminUserStatistics() {
  const response = await apiClient.get<ApiResponse<AdminUserStatistics>>(`${base}/statistics`);
  return unwrap(response.data);
}

export async function updateAdminUser(userId: string, input: UpdateAdminUserInput) {
  const response = await apiClient.put<ApiResponse<AdminUserDetail>>(`${base}/${userId}`, input);
  return unwrap(response.data);
}

export async function updateAdminUserStatus(userId: string, status: UserStatus) {
  const response = await apiClient.patch<ApiResponse<AdminUserDetail>>(`${base}/${userId}/status`, { status });
  return unwrap(response.data);
}

import type { ApiResponse } from "@/lib/api-types";
import { apiClient } from "@/lib/http/api-client";
import { unwrap } from "@/lib/http/unwrap";
import type { ImportUserResult } from "@/features/admin/imports/model/import-contracts";

export async function importAdminUsers(file: File) {
  const form = new FormData();
  form.append("file", file);
  const response = await apiClient.post<ApiResponse<ImportUserResult>>("/v1/api/admin/auth-service/users/import", form, { timeout: 60_000 });
  return unwrap(response.data);
}

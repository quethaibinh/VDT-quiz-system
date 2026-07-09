import type { ApiResponse } from "@/lib/api-types";

export function unwrap<T>(response: ApiResponse<T> | T): T {
  if (response && typeof response === "object" && "data" in response && "status" in response) {
    return (response as ApiResponse<T>).data;
  }
  return response as T;
}

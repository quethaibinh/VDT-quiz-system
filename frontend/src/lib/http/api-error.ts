import axios from "axios";
import type { ApiErrorResponse } from "@/lib/api-types";

export function getApiErrorMessage(error: unknown) {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data?.message ?? "Không thể kết nối đến máy chủ.";
  }
  return error instanceof Error ? error.message : "Đã xảy ra lỗi không xác định.";
}

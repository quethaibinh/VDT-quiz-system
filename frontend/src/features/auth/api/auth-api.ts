import { apiClient } from "@/lib/http/api-client";

export async function login(input: { username: string; password: string }) {
  const response = await apiClient.post<string>("/v1/api/auth-service/login", input, {
    responseType: "text",
    transformResponse: [(value) => value],
  });
  return response.data.replace(/^"|"$/g, "");
}

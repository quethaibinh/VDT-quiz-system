const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

if (!/^(https?:\/\/|\/)/.test(apiBaseUrl)) {
  throw new Error("VITE_API_BASE_URL phải là HTTP URL hoặc đường dẫn tương đối hợp lệ.");
}

export const env = {
  apiBaseUrl,
  enableMocks: import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS === "true",
};

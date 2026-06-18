import axios from "axios";
import { env } from "@/lib/env";

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 20_000,
  headers: { Accept: "application/json" },
});

apiClient.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("sahara.quiz.session");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

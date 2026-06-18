import { QueryClient } from "@tanstack/react-query";
import axios from "axios";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (count, error) => {
        const status = axios.isAxiosError(error) ? error.response?.status : undefined;
        return !status || status >= 500 ? count < 1 : false;
      },
      refetchOnWindowFocus: true,
    },
    mutations: { retry: false },
  },
});

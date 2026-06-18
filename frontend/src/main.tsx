import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { env } from "@/lib/env";
import { AppProviders } from "@/app/providers";
import "@/styles/index.css";

async function bootstrap() {
  if (env.enableMocks) {
    const { worker } = await import("@/mocks/browser");
    await worker.start({ onUnhandledRequest: "bypass" });
  }
  createRoot(document.getElementById("root")!).render(
    <StrictMode><AppProviders /></StrictMode>,
  );
}

void bootstrap();

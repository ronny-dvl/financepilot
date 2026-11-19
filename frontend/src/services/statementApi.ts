import type { FinancialSummaryResponse, StatementAnalysisResponse } from "../types/statement";

const API_URL =
  (import.meta.env.VITE_API_URL?.trim() || "http://localhost:8080").replace(/\/+$/, "");

export async function checkBackendHealth(signal: AbortSignal): Promise<void> {
  const response = await fetch(`${API_URL}/api/health`, { signal, cache: "no-store" });
  if (!response.ok) throw new Error("Servidor indisponível.");
  const body: unknown = await response.json();
  if (!body || typeof body !== "object" || !("status" in body) || body.status !== "ok") {
    throw new Error("Servidor ainda não está pronto.");
  }
}

async function postStatement<T>(path: string, file: File, signal?: AbortSignal): Promise<T> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(
    `${API_URL}/api/statements${path}`,
    {
      method: "POST",
      body: formData,
      signal,
    }
  );

  if (!response.ok) {
    let message = response.status === 413
      ? "O arquivo ultrapassa o limite de 10 MB."
      : "Não foi possível analisar o extrato. Tente novamente.";
    if (response.status === 400 || response.status === 422) {
      const body: unknown = await response.json().catch(() => null);
      if (body && typeof body === "object" && "error" in body
          && typeof body.error === "string" && body.error.trim()) {
        message = body.error;
      }
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function analyzeStatement(file: File, signal?: AbortSignal): Promise<StatementAnalysisResponse> {
  return postStatement<StatementAnalysisResponse>("/analyze", file, signal);
}

export function getFinancialSummary(file: File): Promise<FinancialSummaryResponse> {
  return postStatement<FinancialSummaryResponse>("/summary", file);
}

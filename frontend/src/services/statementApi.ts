import type { FinancialSummaryResponse, StatementAnalysisResponse } from "../types/statement";

const API_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

async function postStatement<T>(path: string, file: File): Promise<T> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(
    `${API_URL}/api/statements${path}`,
    {
      method: "POST",
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error(
      response.status === 413
        ? "O arquivo ultrapassa o limite de 10 MB."
        : "Não foi possível analisar o extrato. Tente novamente."
    );
  }

  return response.json() as Promise<T>;
}

export function analyzeStatement(file: File): Promise<StatementAnalysisResponse> {
  return postStatement<StatementAnalysisResponse>("/analyze", file);
}

export function getFinancialSummary(file: File): Promise<FinancialSummaryResponse> {
  return postStatement<FinancialSummaryResponse>("/summary", file);
}

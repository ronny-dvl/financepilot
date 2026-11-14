const API_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function getFinancialSummary(file: File) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(
    `${API_URL}/api/statements/summary`,
    {
      method: "POST",
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error(
      "Não foi possível analisar o extrato. Tente novamente."
    );
  }

  return response.json();
}
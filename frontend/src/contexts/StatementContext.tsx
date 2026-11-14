import { createContext, useContext, useState, type ReactNode } from "react";
import type { StatementAnalysisResponse } from "../types/statement";

type StatementContextValue = {
  analysis: StatementAnalysisResponse | null;
  setAnalysis: (analysis: StatementAnalysisResponse) => void;
};

const StatementContext = createContext<StatementContextValue | undefined>(undefined);

export function StatementProvider({ children }: { children: ReactNode }) {
  const [analysis, setAnalysis] = useState<StatementAnalysisResponse | null>(null);

  return (
    <StatementContext.Provider value={{ analysis, setAnalysis }}>
      {children}
    </StatementContext.Provider>
  );
}

export function useStatement() {
  const context = useContext(StatementContext);
  if (!context) {
    throw new Error("useStatement deve ser usado dentro de StatementProvider.");
  }
  return context;
}

import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import type { StatementAnalysisResponse } from "../types/statement";

type StatementContextValue = {
  analysis: StatementAnalysisResponse | null;
  setAnalysis: (analysis: StatementAnalysisResponse) => void;
  revealDashboard: boolean;
  markDashboardViewed: () => void;
};

const StatementContext = createContext<StatementContextValue | undefined>(undefined);

export function StatementProvider({ children }: { children: ReactNode }) {
  const [analysis, setAnalysisState] = useState<StatementAnalysisResponse | null>(null);
  const [revealDashboard, setRevealDashboard] = useState(false);
  const setAnalysis = useCallback((result: StatementAnalysisResponse) => {
    setAnalysisState(result);
    setRevealDashboard(true);
  }, []);
  const markDashboardViewed = useCallback(() => setRevealDashboard(false), []);

  return (
    <StatementContext.Provider value={{ analysis, setAnalysis, revealDashboard, markDashboardViewed }}>
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

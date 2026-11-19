import { BrowserRouter, Routes, Route } from "react-router-dom";
import Landing from "./pages/Landing";
import Analisar from "./pages/Analisar";
import Dashboard from "./pages/Dashboard";
import { StatementProvider } from "./contexts/StatementContext";
import { BackendReadinessProvider } from "./contexts/BackendReadinessContext";

export default function App() {
  return (
    <BrowserRouter>
      <StatementProvider>
        <BackendReadinessProvider>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/analisar" element={<Analisar />} />
            <Route path="/dashboard" element={<Dashboard />} />
          </Routes>
        </BackendReadinessProvider>
      </StatementProvider>
    </BrowserRouter>
  );
}

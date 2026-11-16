import { BrowserRouter, Routes, Route } from "react-router-dom";
import Landing from "./pages/Landing";
import Analisar from "./pages/Analisar";
import Dashboard from "./pages/Dashboard";
import { StatementProvider } from "./contexts/StatementContext";

export default function App() {
  return (
    <BrowserRouter>
      <StatementProvider>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/analisar" element={<Analisar />} />
          <Route path="/dashboard" element={<Dashboard />} />
        </Routes>
      </StatementProvider>
    </BrowserRouter>
  );
}

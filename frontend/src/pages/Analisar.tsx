
import { useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import PdfUpload from "../components/PdfUpload/PdfUpload";
import ThemeSelector from "../components/ThemeSelector/ThemeSelector";
import AuthOverlay from "../components/AuthOverlay";
import { useStatement } from "../contexts/StatementContext";
import { useBackendReadiness } from "../contexts/BackendReadinessContext";
import BackendStartupWait from "../components/BackendStartupWait/BackendStartupWait";
import styles from "./Analisar.module.css";

export default function Analisar() {
  const { guestFirstName } = useStatement();
  const navigate = useNavigate();
  const { status, ensureReady, cancel } = useBackendReadiness();
  useEffect(() => {
    ensureReady();
    return () => cancel();
  }, [ensureReady, cancel]);
  if (!guestFirstName) {
    return <main className={styles.page}><AuthOverlay open onClose={() => navigate("/")} /></main>;
  }
  if (status !== "ready") {
    return (
      <main className={styles.page}>
        <div className={styles.container}>
          <header className={styles.topbar}>
            <Link to="/" className={styles.back}><ArrowLeft size={18} aria-hidden="true" />Voltar</Link>
            <ThemeSelector />
          </header>
          <div className={styles.waitingArea}>
            {(status === "waiting" || status === "error") && <BackendStartupWait failed={status === "error"}
              onRetry={() => ensureReady(true)} onCancel={() => { cancel(); navigate("/"); }} />}
          </div>
        </div>
      </main>
    );
  }
  return (
    <main className={styles.page}>
      <div className={styles.container}>
        <header className={styles.topbar}>
          <Link to="/" className={styles.back}>
            <ArrowLeft size={18} />
            Voltar
          </Link>
          <ThemeSelector />
        </header>

        <div className={styles.content}>
          <div className={styles.heading}>
            <span className={styles.eyebrow}>
              FINANCEPILOT
            </span>

            <h1>
              Entenda para onde seu{" "}
              <span>dinheiro está indo.</span>
            </h1>

            <p>
              Envie seu extrato bancário em PDF e
              transforme suas movimentações em
              informações claras sobre suas finanças.
            </p>
          </div>

          <div className={styles.uploadArea}>
            <PdfUpload />
          </div>
        </div>
      </div>
    </main>
  );
}

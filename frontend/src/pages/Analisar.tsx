
import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import PdfUpload from "../components/PdfUpload/PdfUpload";
import ThemeSelector from "../components/ThemeSelector/ThemeSelector";
import styles from "./Analisar.module.css";

export default function Analisar() {
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

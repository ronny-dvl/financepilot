
import { Link, useLocation } from "react-router-dom";
import styles from "./Pages.module.css";

type FinancialSummary = {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  transactionCount: number;
};

export default function Dashboard() {
  const location = useLocation();

  const summary = (
    location.state as { summary?: FinancialSummary } | null
  )?.summary;

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);

  if (!summary) {
    return (
      <main className={styles.center}>
        <h1>Nenhum extrato analisado</h1>
        <p>Envie seu extrato bancário para visualizar suas finanças.</p>
        <Link to="/analisar">Analisar meu extrato</Link>
      </main>
    );
  }

  return (
    <main className={styles.center}>
      <h1>Seu painel de gastos</h1>
      <p>Confira o resumo financeiro do seu extrato.</p>

      <section>
        <h2>Resumo financeiro</h2>

        <div>
          <h3>Entradas</h3>
          <p>{formatCurrency(summary.totalIncome)}</p>
        </div>

        <div>
          <h3>Saídas</h3>
          <p>{formatCurrency(summary.totalExpenses)}</p>
        </div>

        <div>
          <h3>Saldo líquido</h3>
          <p>{formatCurrency(summary.netBalance)}</p>
        </div>

        <div>
          <h3>Transações</h3>
          <p>{summary.transactionCount}</p>
        </div>
      </section>

      <Link to="/analisar">Analisar outro extrato</Link>
    </main>
  );
}

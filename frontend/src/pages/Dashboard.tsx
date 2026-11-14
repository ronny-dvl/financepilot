
import { Link } from "react-router-dom";
import { useStatement } from "../contexts/StatementContext";
import type { TransactionCategory } from "../types/statement";
import styles from "./Pages.module.css";

const categoryLabels: Record<TransactionCategory, string> = {
  ALIMENTACAO: "Alimentação",
  TRANSPORTE: "Transporte",
  SAUDE: "Saúde",
  ASSINATURAS: "Assinaturas",
  MORADIA: "Moradia",
  EDUCACAO: "Educação",
  LAZER: "Lazer",
  COMPRAS: "Compras",
  TRANSFERENCIAS: "Transferências",
  OUTROS: "Outros",
};

export default function Dashboard() {
  const { analysis } = useStatement();

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);

  if (!analysis) {
    return (
      <main className={styles.center}>
        <h1>Nenhum extrato analisado</h1>
        <p>Envie seu extrato bancário para visualizar suas finanças.</p>
        <Link to="/analisar">Analisar meu extrato</Link>
      </main>
    );
  }

  const { summary, transactions, categorySummary } = analysis;

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

      <section aria-labelledby="categories-heading">
        <h2 id="categories-heading">Categorias</h2>
        <dl>
          <dt>Total de saídas</dt>
          <dd>{formatCurrency(categorySummary.totalOutflow)}</dd>
          <dt>Despesas categorizadas</dt>
          <dd>{formatCurrency(categorySummary.categorizedExpenses)}</dd>
          <dt>Saídas não categorizadas</dt>
          <dd>{formatCurrency(categorySummary.uncategorizedOutflow)}</dd>
          <dt>Transferências entre contas próprias</dt>
          <dd>{formatCurrency(categorySummary.transferOutflow)}</dd>
        </dl>
        {Object.keys(categorySummary.expensesByCategory).length === 0 ? (
          <p>Nenhuma despesa categorizada neste extrato.</p>
        ) : (
          <dl>
            {Object.entries(categoryLabels).map(([category, label]) => {
              const amount = categorySummary.expensesByCategory[category as TransactionCategory];
              return amount === undefined ? null : (
                <div key={category}>
                  <dt>{label}</dt>
                  <dd>{formatCurrency(amount)}</dd>
                </div>
              );
            })}
          </dl>
        )}
      </section>

      <section aria-labelledby="transactions-heading">
        <h2 id="transactions-heading">Transações</h2>
        {transactions.length === 0 ? (
          <p>Nenhuma transação reconhecida neste extrato.</p>
        ) : (
          <ul>
            {transactions.map((transaction, index) => (
              <li key={index}>
                <time dateTime={transaction.date}>
                  {transaction.date.split("-").reverse().join("/")}
                </time>
                {" — "}{transaction.description}
                {" — "}{formatCurrency(transaction.amount)}
                {" — "}{categoryLabels[transaction.category]}
                {transaction.requiresReview && " (Requer revisão)"}
              </li>
            ))}
          </ul>
        )}
      </section>

      <Link to="/analisar">Analisar outro extrato</Link>
    </main>
  );
}

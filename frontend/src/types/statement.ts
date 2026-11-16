export type TransactionCategory =
  | "ALIMENTACAO" | "TRANSPORTE" | "SAUDE" | "ASSINATURAS"
  | "MORADIA" | "EDUCACAO" | "LAZER" | "COMPRAS"
  | "TRANSFERENCIAS" | "OUTROS";

export type CategorizationSource =
  | "USER_RULE" | "MERCHANT_DATABASE" | "KEYWORD_RULE"
  | "CNAE" | "AI" | "UNKNOWN";

export type TransactionNature =
  | "EXPENSE" | "INCOME" | "TRANSFER" | "REFUND" | "UNKNOWN";

export type PaymentMethod =
  | "PIX" | "CREDIT_CARD" | "DEBIT_CARD" | "BANK_TRANSFER"
  | "BOLETO" | "CASH" | "UNKNOWN";

export type FinancialSummaryResponse = {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  transactionCount: number;
};

export type TransactionResponse = {
  date: string;
  description: string;
  amount: number;
  type: "INCOME" | "EXPENSE";
  category: TransactionCategory;
  source: CategorizationSource;
  requiresReview: boolean;
  nature: TransactionNature;
  paymentMethod: PaymentMethod;
};

export type CategorySummaryResponse = {
  totalOutflow: number;
  categorizedExpenses: number;
  uncategorizedOutflow: number;
  transferOutflow: number;
  expensesByCategory: Partial<Record<TransactionCategory, number>>;
};

export type StatementAnalysisResponse = {
  summary: FinancialSummaryResponse;
  transactions: TransactionResponse[];
  categorySummary: CategorySummaryResponse;
};

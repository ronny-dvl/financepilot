import type { PaymentMethod, TransactionCategory, TransactionResponse } from "../../types/statement";

export const categoryLabels: Record<TransactionCategory, string> = {
  ALIMENTACAO: "Alimentação", TRANSPORTE: "Transporte", SAUDE: "Saúde",
  ASSINATURAS: "Assinaturas", MORADIA: "Moradia", EDUCACAO: "Educação",
  LAZER: "Lazer", COMPRAS: "Compras", TRANSFERENCIAS: "Transferências", OUTROS: "Outros",
  SUPERMERCADO: "Supermercado", CONTAS_ESSENCIAIS: "Contas essenciais",
  INTERNET_TELEFONIA: "Internet e telefonia", GAMES: "Jogos", TECNOLOGIA: "Tecnologia",
  VESTUARIO: "Vestuário e acessórios", CASA_UTILIDADES: "Casa e utilidades",
  ESPORTE_BEM_ESTAR: "Esporte e bem-estar",
  BELEZA_CUIDADOS_PESSOAIS: "Beleza e cuidados pessoais", PETS: "Pets", VIAGENS: "Viagens",
  INVESTIMENTOS: "Investimentos", TARIFAS_BANCARIAS: "Tarifas bancárias",
  IMPOSTOS_TRIBUTOS: "Impostos e tributos",
  TRANSFERENCIAS_PESSOAIS: "Transferências pessoais",
};

export const paymentLabels: Record<PaymentMethod, string> = {
  PIX: "PIX", CREDIT_CARD: "Cartão de crédito", DEBIT_CARD: "Cartão de débito",
  BANK_TRANSFER: "Transferência bancária", BOLETO: "Boleto", CASH: "Dinheiro",
  UNKNOWN: "Não identificado",
};

export const currency = (value: number) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);

// API dates have no time or timezone. UTC is used only for calendar arithmetic.
export function dateLabel(value: string, short = false) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit", month: short ? "2-digit" : "long",
    ...(short ? {} : { year: "numeric" as const }), timeZone: "UTC",
  }).format(new Date(`${value}T00:00:00Z`));
}

export const normalizeSearch = (value: string) =>
  value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("pt-BR").trim();

export function groupByDate(transactions: TransactionResponse[]) {
  const groups = new Map<string, TransactionResponse[]>();
  for (const transaction of transactions) {
    const group = groups.get(transaction.date) ?? [];
    group.push(transaction);
    groups.set(transaction.date, group);
  }
  return [...groups.entries()].sort(([a], [b]) => b.localeCompare(a));
}

type Week = { start: string; end: string; income: number; outflow: number };
const isoDate = (date: Date) => date.toISOString().slice(0, 10);

// Presentation only: group the signed movements returned by the API into calendar weeks.
// Integer cents avoid floating-point drift when adding amounts for the chart.
export function groupByWeek(transactions: TransactionResponse[]): Week[] {
  const groups = new Map<string, Week>();
  for (const transaction of transactions) {
    const monday = new Date(`${transaction.date}T00:00:00Z`);
    monday.setUTCDate(monday.getUTCDate() - (monday.getUTCDay() + 6) % 7);
    const start = isoDate(monday);
    const sunday = new Date(monday);
    sunday.setUTCDate(sunday.getUTCDate() + 6);
    const week = groups.get(start) ?? { start, end: isoDate(sunday), income: 0, outflow: 0 };
    const cents = Math.round(transaction.amount * 100);
    if (cents > 0) week.income += cents;
    if (cents < 0) week.outflow += Math.abs(cents);
    groups.set(start, week);
  }
  return [...groups.values()].sort((a, b) => a.start.localeCompare(b.start))
    .map(week => ({ ...week, income: week.income / 100, outflow: week.outflow / 100 }));
}

import { useMemo } from "react";
import {
  ArrowLeftRight, Building2, Car, Dumbbell, Gamepad2, GraduationCap, HeartPulse,
  House, Landmark, Laptop, Layers, Lightbulb, PawPrint, Plane, ReceiptText,
  Repeat2, Shirt, ShoppingBag, ShoppingCart, Sparkles, Ticket, TrendingUp,
  Utensils, Wifi, type LucideIcon,
} from "lucide-react";
import type { TransactionCategory, TransactionResponse } from "../../types/statement";
import { categoryLabels } from "../Dashboard/presentation";
import TransactionDetailsModal from "./TransactionDetailsModal";

const categoryIcons: Record<TransactionCategory, LucideIcon> = {
  ALIMENTACAO: Utensils, TRANSPORTE: Car, SAUDE: HeartPulse, ASSINATURAS: Repeat2,
  MORADIA: Building2, EDUCACAO: GraduationCap, LAZER: Ticket, COMPRAS: ShoppingBag,
  TRANSFERENCIAS: ArrowLeftRight, OUTROS: Layers, SUPERMERCADO: ShoppingCart,
  CONTAS_ESSENCIAIS: Lightbulb, INTERNET_TELEFONIA: Wifi, GAMES: Gamepad2,
  TECNOLOGIA: Laptop, VESTUARIO: Shirt, ESPORTE_BEM_ESTAR: Dumbbell,
  BELEZA_CUIDADOS_PESSOAIS: Sparkles, PETS: PawPrint, VIAGENS: Plane,
  INVESTIMENTOS: TrendingUp, TARIFAS_BANCARIAS: Landmark, IMPOSTOS_TRIBUTOS: ReceiptText,
  CASA_UTILIDADES: House, TRANSFERENCIAS_PESSOAIS: ArrowLeftRight,
};

type Props = {
  category: TransactionCategory;
  total: number;
  transactions: readonly TransactionResponse[];
  onClose: () => void;
  returnFocusTo?: HTMLElement | null;
};

export default function CategoryDetailsModal({ category, total, transactions, onClose, returnFocusTo }: Props) {
  const label = categoryLabels[category] ?? category;
  const Icon = categoryIcons[category] ?? Layers;
  // Presentation-only membership selection from the same fields CategorySummaryService uses.
  // Personal transfers belong to the chart with TRANSFER nature; the API owns the total.
  const movements = useMemo(() => transactions.filter(transaction => {
    if (transaction.category !== category || !Number.isFinite(transaction.amount)
        || transaction.amount >= 0 || transaction.requiresReview !== false) return false;
    if (category === "TRANSFERENCIAS_PESSOAIS") return transaction.nature === "TRANSFER";
    return category !== "OUTROS" && category !== "INVESTIMENTOS" && category !== "TRANSFERENCIAS"
      && transaction.nature === "EXPENSE";
  }), [category, transactions]);

  return <TransactionDetailsModal title={label} subtitle="Detalhamento da categoria"
    totalLabel="Total da categoria" total={total} transactions={movements} icon={Icon}
    onClose={onClose} returnFocusTo={returnFocusTo} />;
}

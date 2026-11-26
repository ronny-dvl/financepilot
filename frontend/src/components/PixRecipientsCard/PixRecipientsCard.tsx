import { useCallback, useMemo, useRef, useState } from "react";
import { AnimatePresence } from "framer-motion";
import { ArrowLeftRight, ChevronDown, ChevronRight, ChevronUp, Users } from "lucide-react";
import type { PixRecipient, RecipientSummaryResponse, TransactionResponse } from "../../types/statement";
import { currency } from "../Dashboard/presentation";
import TransactionDetailsModal from "../CategoryDetailsModal/TransactionDetailsModal";
import styles from "./PixRecipientsCard.module.css";

type Props = {
  summary?: RecipientSummaryResponse;
  transactions: readonly TransactionResponse[];
  headingId: string;
};

function initials(name: string) {
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.slice(0, 1) ?? "") + (parts.length > 1 ? parts[parts.length - 1].slice(0, 1) : ""))
    .toLocaleUpperCase("pt-BR");
}

export default function PixRecipientsCard({ summary, transactions, headingId }: Props) {
  const [sort, setSort] = useState<"value" | "count">("value");
  const [expanded, setExpanded] = useState(false);
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const closeDetails = useCallback(() => setSelectedName(null), []);
  const recipients = useMemo(() => [...(summary?.recipients ?? [])].sort((a, b) =>
    (sort === "value" ? b.totalSent - a.totalSent : b.transferCount - a.transferCount)
    || (sort === "value" ? b.transferCount - a.transferCount : b.totalSent - a.totalSent)
    || a.name.localeCompare(b.name, "pt-BR")), [summary, sort]);
  const selected = recipients.find(recipient => recipient.name === selectedName);
  const selectedTransactions = useMemo(() => {
    if (!selected) return [];
    const seen = new Set<number>();
    return selected.transactionIndexes.flatMap(index => {
      if (!Number.isInteger(index) || index < 0 || seen.has(index)) return [];
      const transaction = transactions[index];
      if (!transaction || transaction.category !== "TRANSFERENCIAS_PESSOAIS"
          || transaction.nature !== "TRANSFER" || transaction.paymentMethod !== "PIX"
          || transaction.requiresReview !== false || transaction.amount >= 0) return [];
      seen.add(index);
      return [transaction];
    });
  }, [selected, transactions]);
  const visible = expanded ? recipients : recipients.slice(0, 5);
  const maximum = recipients.reduce((max, recipient) =>
    Math.max(max, sort === "value" ? recipient.totalSent : recipient.transferCount), 0) || 1;
  const openDetails = (recipient: PixRecipient, trigger: HTMLButtonElement) => {
    triggerRef.current = trigger;
    setSelectedName(recipient.name);
  };

  return (
    <div className={styles.card}>
      <div className={styles.heading}>
        <span className={styles.mainIcon}><Users size={22} aria-hidden="true" /></span>
        <div><h2 id={headingId}>Destinatários PIX</h2><p>Para quem você mais transferiu dinheiro</p></div>
      </div>

      {summary && (
        <div className={styles.summary}>
          <div><span>Total transferido</span><strong className={styles.total}>{currency(summary.totalSent)}</strong></div>
          <div><span>Destinatários</span><strong>{summary.recipientCount.toLocaleString("pt-BR")}</strong></div>
        </div>
      )}

      {recipients.length === 0 ? (
        <p className={styles.empty}>{summary
          ? "Nenhum destinatário identificado nos PIX de saída classificados como transferências pessoais."
          : "Os destinatários PIX não estão disponíveis nesta análise."}</p>
      ) : (
        <>
          <div className={styles.sort} role="group" aria-label="Ordenar destinatários PIX">
            <button type="button" aria-pressed={sort === "value"} onClick={() => setSort("value")}>Maior valor</button>
            <button type="button" aria-pressed={sort === "count"} onClick={() => setSort("count")}>Mais transferências</button>
          </div>

          <ol id={`${headingId}-ranking`} className={styles.ranking} aria-label="Ranking de destinatários PIX">
            {visible.map((recipient, index) => (
              <li key={recipient.name}>
                <button type="button" className={styles.row} aria-haspopup="dialog"
                  aria-expanded={selectedName === recipient.name}
                  aria-label={`Ver transferências para ${recipient.name}: ${currency(recipient.totalSent)}, ${recipient.transferCount} PIX`}
                  onClick={event => openDetails(recipient, event.currentTarget)}>
                  <span className={styles.position} aria-hidden="true">{String(index + 1).padStart(2, "0")}</span>
                  <span className={styles.avatar} aria-hidden="true">{initials(recipient.name)}</span>
                  <span className={styles.name}><strong>{recipient.name}</strong>
                    <span>{recipient.transferCount.toLocaleString("pt-BR")} {recipient.transferCount === 1 ? "transferência" : "transferências"}</span>
                  </span>
                  <strong className={styles.amount}>{currency(recipient.totalSent)}</strong>
                  <ChevronRight size={17} className={styles.chevron} aria-hidden="true" />
                  <span className={styles.track} aria-hidden="true"><span style={{
                    width: `${(sort === "value" ? recipient.totalSent : recipient.transferCount) / maximum * 100}%`,
                  }} /></span>
                </button>
              </li>
            ))}
          </ol>

          {recipients.length > 5 && (
            <button type="button" className={styles.expand} aria-expanded={expanded} aria-controls={`${headingId}-ranking`} onClick={() => setExpanded(value => !value)}>
              {expanded ? "Mostrar menos" : "Ver todos os destinatários"}
              {expanded ? <ChevronUp size={17} aria-hidden="true" /> : <ChevronDown size={17} aria-hidden="true" />}
            </button>
          )}
        </>
      )}

      <AnimatePresence>
        {selected && (
          <TransactionDetailsModal key={selected.name} title={selected.name} subtitle="Detalhamento das transferências"
            totalLabel="Total enviado" total={selected.totalSent} transactions={selectedTransactions}
            expectedCount={selected.transferCount} listTitle="Transferências" countUnit="PIX"
            avatarText={initials(selected.name)} icon={ArrowLeftRight} onClose={closeDetails} returnFocusTo={triggerRef.current} />
        )}
      </AnimatePresence>
    </div>
  );
}

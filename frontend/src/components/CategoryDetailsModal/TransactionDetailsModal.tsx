import { useId, useLayoutEffect, useMemo, useRef } from "react";
import { createPortal } from "react-dom";
import { motion, useReducedMotion } from "framer-motion";
import { X, type LucideIcon } from "lucide-react";
import type { TransactionResponse } from "../../types/statement";
import { currency, dateLabel, paymentLabels } from "../Dashboard/presentation";
import styles from "./CategoryDetailsModal.module.css";

type Props = {
  title: string;
  subtitle: string;
  totalLabel: string;
  total: number;
  transactions: readonly TransactionResponse[];
  icon: LucideIcon;
  listTitle?: string;
  countUnit?: "movimentação" | "PIX";
  expectedCount?: number;
  avatarText?: string;
  onClose: () => void;
  returnFocusTo?: HTMLElement | null;
};

/** Shared frame, focus management and transaction list for category and recipient details. */
export default function TransactionDetailsModal({
  title, subtitle, totalLabel, total, transactions, icon: Icon, listTitle = "Movimentações",
  countUnit = "movimentação", expectedCount, avatarText, onClose, returnFocusTo,
}: Props) {
  const reducedMotion = useReducedMotion();
  const id = useId();
  const overlayRef = useRef<HTMLDivElement>(null);
  const dialogRef = useRef<HTMLElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const duration = reducedMotion ? 0 : 0.2;
  const movements = useMemo(() => [...transactions].sort((a, b) => b.date.localeCompare(a.date)), [transactions]);
  const amountsInCents = movements.map(transaction => Math.round(Math.abs(transaction.amount) * 100));
  const sumInCents = amountsInCents.reduce((sum, amount) => sum + amount, 0);
  const totalInCents = Math.round(total * 100);
  const reconciled = Number.isSafeInteger(totalInCents) && Number.isSafeInteger(sumInCents)
    && movements.every(transaction => Number.isFinite(transaction.amount) && transaction.amount < 0)
    && amountsInCents.every(Number.isSafeInteger) && sumInCents === totalInCents
    && (expectedCount === undefined || expectedCount === movements.length);

  useLayoutEffect(() => {
    const dialog = dialogRef.current;
    const overlay = overlayRef.current;
    if (!dialog || !overlay) return;
    const previousFocus = returnFocusTo ?? (document.activeElement instanceof HTMLElement ? document.activeElement : null);
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const body = document.body;
    const previousStyle = {
      overflow: body.style.overflow, position: body.style.position,
      top: body.style.top, left: body.style.left, width: body.style.width,
      paddingRight: body.style.paddingRight,
    };
    const scrollbarWidth = Math.max(0, window.innerWidth - document.documentElement.clientWidth);
    const paddingRight = parseFloat(window.getComputedStyle(body).paddingRight) || 0;
    Object.assign(body.style, {
      overflow: "hidden", position: "fixed", top: `-${scrollY}px`,
      left: `-${scrollX}px`, width: "100%", paddingRight: `${paddingRight + scrollbarWidth}px`,
    });

    // Move focus before hiding the dashboard from assistive technology.
    closeRef.current?.focus({ preventScroll: true });
    const background = Array.from(body.children)
      .filter((element): element is HTMLElement => element instanceof HTMLElement && element !== overlay)
      .map(element => ({ element, inert: element.inert, ariaHidden: element.getAttribute("aria-hidden") }));
    background.forEach(({ element }) => { element.inert = true; element.setAttribute("aria-hidden", "true"); });

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = Array.from(dialog.querySelectorAll<HTMLElement>(
        'button:not(:disabled), a[href], [tabindex]:not([tabindex="-1"])'
      )).filter(element => element.getClientRects().length > 0);
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (!first || !last) { event.preventDefault(); dialog.focus(); return; }
      if (event.shiftKey && (document.activeElement === first || !dialog.contains(document.activeElement))) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !dialog.contains(document.activeElement))) {
        event.preventDefault(); first.focus();
      }
    };
    const keepFocusInside = (event: FocusEvent) => {
      if (event.target instanceof Node && !dialog.contains(event.target)) closeRef.current?.focus({ preventScroll: true });
    };
    document.addEventListener("keydown", onKeyDown, true);
    document.addEventListener("focusin", keepFocusInside);
    return () => {
      document.removeEventListener("keydown", onKeyDown, true);
      document.removeEventListener("focusin", keepFocusInside);
      background.forEach(({ element, inert, ariaHidden }) => {
        element.inert = inert;
        if (ariaHidden === null) element.removeAttribute("aria-hidden");
        else element.setAttribute("aria-hidden", ariaHidden);
      });
      Object.assign(body.style, previousStyle);
      const previousScrollBehavior = document.documentElement.style.scrollBehavior;
      document.documentElement.style.scrollBehavior = "auto";
      if (previousFocus?.isConnected) window.scrollTo(scrollX, scrollY);
      document.documentElement.style.scrollBehavior = previousScrollBehavior;
      if (previousFocus?.isConnected) previousFocus.focus({ preventScroll: true });
    };
  }, [onClose, returnFocusTo]);

  return createPortal(
    <motion.div ref={overlayRef} className={styles.backdrop}
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      transition={{ duration }}
      onClick={event => { if (event.target === event.currentTarget) onClose(); }}>
      <motion.section ref={dialogRef} className={styles.modal}
        role="dialog" aria-modal="true" aria-labelledby={`${id}-title`} aria-describedby={`${id}-subtitle`}
        tabIndex={-1} initial={reducedMotion ? false : { opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: reducedMotion ? 1 : 0.96 }}
        transition={{ duration, ease: "easeOut" }}>
        <header className={styles.header}>
          <span className={`${styles.categoryIcon} ${avatarText ? styles.avatarIcon : ""}`} aria-hidden="true">{avatarText ?? <Icon size={23} aria-hidden="true" />}</span>
          <div className={styles.heading}>
            <h2 id={`${id}-title`}>{title}</h2>
            <p id={`${id}-subtitle`}>{subtitle}</p>
          </div>
          <button ref={closeRef} type="button" onClick={onClose} className={styles.closeIcon} aria-label={`Fechar ${subtitle.toLocaleLowerCase("pt-BR")}`}>
            <X size={20} aria-hidden="true" />
          </button>
        </header>

        <div className={styles.totalCard}>
          <p>{totalLabel}</p>
          <strong>{currency(total)}</strong>
        </div>

        <section className={styles.movements} aria-labelledby={`${id}-movements`}>
          <div className={styles.listHeading}>
            <h3 id={`${id}-movements`}>{listTitle}</h3>
            <span>{reconciled ? `${movements.length.toLocaleString("pt-BR")} ${countUnit === "PIX" ? "PIX" : movements.length === 1 ? "movimentação" : "movimentações"}` : "Quantidade indisponível"}</span>
          </div>
          <div className={styles.listScroll} tabIndex={0} role="region" aria-label={`${listTitle} de ${title}`}>
            {!reconciled ? (
              <p className={styles.empty} role="status">Não foi possível conciliar as movimentações disponíveis com o total informado pela análise.</p>
            ) : movements.length === 0 ? (
              <p className={styles.empty}>Nenhuma movimentação disponível para este detalhamento.</p>
            ) : (
              <ul className={styles.list}>
                {movements.map((transaction, index) => (
                  <li className={styles.movement} key={`${transaction.date}-${index}`}>
                    <span className={styles.movementIcon}><Icon size={18} aria-hidden="true" /></span>
                    <div className={styles.description}>
                      <p>{transaction.description}</p>
                      <div className={styles.metadata}>
                        <time dateTime={transaction.date}>{dateLabel(transaction.date)}</time>
                        <span aria-hidden="true">·</span>
                        <span>{paymentLabels[transaction.paymentMethod] ?? "Não identificado"}</span>
                      </div>
                    </div>
                    <strong className={styles.amount}>{currency(transaction.amount)}</strong>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <footer className={styles.footer}>
          <button type="button" className={styles.closeButton} onClick={onClose}>Fechar</button>
        </footer>
      </motion.section>
    </motion.div>,
    document.body,
  );
}

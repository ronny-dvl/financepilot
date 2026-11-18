import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { motion, useReducedMotion } from "framer-motion";
import {
  ArrowDownLeft, ArrowUpRight, ArrowRight, ArrowLeft, Wallet,
  ListFilter, Search, CalendarDays, ChartColumn, Layers, Users, FileText, Plus,
} from "lucide-react";
import { useStatement } from "../contexts/StatementContext";
import {
  categoryLabels, paymentLabels, currency, dateLabel,
  groupByDate, groupByWeek, normalizeSearch,
} from "../components/Dashboard/presentation";
import type { TransactionCategory } from "../types/statement";
import logo from "../assets/logo.svg";
import ThemeSelector from "../components/ThemeSelector/ThemeSelector";
import styles from "./Dashboard.module.css";

function Panel({ id, children }: { id: string; children: ReactNode }) {
  const reducedMotion = useReducedMotion();
  return (
    <motion.section
      aria-labelledby={id}
      className={styles.panel}
      initial={false}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.05 }}
      transition={{ duration: reducedMotion ? 0 : 0.4 }}
    >
      {children}
    </motion.section>
  );
}

export default function Dashboard() {
  const { analysis, revealDashboard, markDashboardViewed, guestFirstName } = useStatement();
  const [reveal] = useState(revealDashboard);
  useEffect(() => { if (analysis) markDashboardViewed(); }, [analysis, markDashboardViewed]);
  const reducedMotion = useReducedMotion();
  const [query, setQuery] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [category, setCategory] = useState("");
  const [type, setType] = useState("");
  const [payment, setPayment] = useState("");
  const invalidPeriod = Boolean(startDate && endDate && startDate > endDate);
  const hasFilters = Boolean(query || startDate || endDate || category || type || payment);

  const dates = useMemo(() =>
    analysis?.transactions.map(transaction => transaction.date).sort() ?? [], [analysis]);
  const weeks = useMemo(() => groupByWeek(analysis?.transactions ?? []), [analysis]);
  const categories = useMemo(() =>
    Object.entries(analysis?.categorySummary.expensesByCategory ?? {})
      .sort(([, a], [, b]) => b - a), [analysis]);
  const filtered = useMemo(() => {
    if (!analysis || invalidPeriod) return [];
    const search = normalizeSearch(query);
    return analysis.transactions.filter(transaction =>
      (!search || normalizeSearch(transaction.description).includes(search)) &&
      (!startDate || transaction.date >= startDate) &&
      (!endDate || transaction.date <= endDate) &&
      (!category || transaction.category === category) &&
      (!type || transaction.type === type) &&
      (!payment || transaction.paymentMethod === payment));
  }, [analysis, query, startDate, endDate, category, type, payment, invalidPeriod]);
  const grouped = useMemo(() => groupByDate(filtered), [filtered]);

  const clearFilters = () => {
    setQuery(""); setStartDate(""); setEndDate("");
    setCategory(""); setType(""); setPayment("");
  };
  const weeklyMax = Math.max(0, ...weeks.flatMap(week => [week.income, week.outflow])) || 1;
  const categoryMax = Math.max(0, ...categories.map(([, amount]) => amount)) || 1;
  const firstName = (guestFirstName || analysis?.holderName)?.trim().split(/\s+/)[0]
    ?.toLocaleLowerCase("pt-BR")
    .replace(/(^|[-'’])(\p{L})/gu, (_, separator: string, letter: string) =>
      separator + letter.toLocaleUpperCase("pt-BR"));

  return (
    <div className={styles.page}>
      <a className={styles.skipLink} href="#dashboard-main">Pular para o conteúdo</a>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <Link to="/" aria-label="FinancePilot — início" className={styles.logo}>
            <img src={logo} alt="FinancePilot" />
          </Link>
          <span className={styles.headerLabel}>Seu painel financeiro</span>
          <ThemeSelector />
          <Link to="/analisar" className={`${styles.primaryButton} ${styles.analyzeAnother}`}>
            <Plus size={17} aria-hidden="true" /> Analisar outro extrato
          </Link>
        </div>
      </header>

      <motion.main id="dashboard-main" className={styles.main}
        initial={reveal && !reducedMotion ? { opacity: 0 } : false}
        animate={{ opacity: 1 }} transition={{ duration: reducedMotion ? 0 : 0.35 }}>
        {!analysis ? (
          <div className={styles.noAnalysis}>
            <span className={styles.emptyIcon}><FileText size={30} aria-hidden="true" /></span>
            <p className={styles.eyebrow}>COMECE PELO SEU EXTRATO</p>
            <h1>Suas finanças, com mais clareza.</h1>
            <p>Nenhum extrato analisado nesta sessão. Envie um PDF para visualizar seu resumo e explorar suas movimentações.</p>
            <Link to="/analisar" className={styles.primaryButton}>Analisar meu extrato <ArrowRight size={18} aria-hidden="true" /></Link>
          </div>
        ) : (
          <>
            <div className={styles.intro}>
              <h1>Olá{firstName ? <>, <span>{firstName}</span></> : null}!</h1>
              {dates.length > 0 && (
                <div className={styles.period}>
                  <CalendarDays size={18} aria-hidden="true" />
                  <div><span>Período das movimentações</span>
                    <strong>{dateLabel(dates[0])} a {dateLabel(dates[dates.length - 1])}</strong>
                  </div>
                </div>
              )}
            </div>

            <section aria-labelledby="summary-heading" className={styles.summary}>
              <h2 id="summary-heading" className={styles.sectionTitle}>Resumo financeiro</h2>
              <div className={styles.cards}>
                {[
                  { label: "Entradas", value: currency(analysis.summary.totalIncome), note: "Total de valores recebidos", icon: ArrowDownLeft, tone: "income" },
                  { label: "Saídas", value: currency(analysis.summary.totalExpenses), note: "Inclui transferências", icon: ArrowUpRight, tone: "outflow" },
                  { label: "Saldo líquido", value: currency(analysis.summary.netBalance), note: "Entradas menos saídas do extrato", icon: Wallet, tone: "balance" },
                  { label: "Transações", value: analysis.summary.transactionCount.toLocaleString("pt-BR"), note: "Movimentações reconhecidas", icon: ListFilter, tone: "count" },
                ].map(({ label, value, note, icon: Icon, tone }, index) => (
                  <motion.article key={label} className={`${styles.card} ${styles[tone]}`}
                    initial={!reveal || reducedMotion ? false : { opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: reducedMotion ? 0 : 0.35, delay: !reveal || reducedMotion ? 0 : index * 0.06 }}>
                    <div className={styles.cardTop}><h3>{label}</h3><span className={styles.cardIcon}><Icon size={20} aria-hidden="true" /></span></div>
                    <p className={styles.cardValue}>{value}</p>
                    <p className={styles.cardNote}>{note}</p>
                  </motion.article>
                ))}
              </div>
              <p className={styles.smallNote}>O saldo líquido representa a movimentação do período, não o saldo da conta bancária.</p>
            </section>

            <Panel id="categories-heading">
              <div className={styles.panelHeading}>
                <div className={styles.titleGroup}><span className={styles.sectionIcon}><Layers size={21} aria-hidden="true" /></span>
                  <div><h2 id="categories-heading">Gastos por categoria</h2></div>
                </div>
                <div className={styles.headingTotal}><span>Despesas categorizadas</span><strong>{currency(analysis.categorySummary.categorizedExpenses)}</strong></div>
              </div>
              {categories.length === 0 ? (
                <div className={styles.empty}><Layers size={25} aria-hidden="true" /><h3>Nenhuma despesa categorizada</h3><p>As saídas sem classificação confiável permanecem separadas abaixo.</p></div>
              ) : (
                <ol className={styles.categoryList} aria-label="Categorias por valor gasto">
                  {categories.map(([key, amount]) => (
                    <li key={key} className={styles.categoryRow}>
                      <div className={styles.categoryLabel}><span>{categoryLabels[key as TransactionCategory] ?? key}</span><strong>{currency(amount)}</strong></div>
                      <div className={styles.track} aria-hidden="true"><div className={styles.categoryBar} style={{ width: `${amount / categoryMax * 100}%` }} /></div>
                    </li>
                  ))}
                </ol>
              )}
              <div className={styles.categoryNotes}>
                <div><span>Saídas não categorizadas</span><strong>{currency(analysis.categorySummary.uncategorizedOutflow)}</strong></div>
                <div><span>Transferências entre contas próprias</span><strong>{currency(analysis.categorySummary.transferOutflow)}</strong></div>
              </div>
            </Panel>

            <Panel id="pix-heading">
              <div className={styles.panelHeading}><div className={styles.titleGroup}>
                <span className={styles.sectionIcon}><Users size={21} aria-hidden="true" /></span>
                <div><h2 id="pix-heading">Destinatários PIX</h2><p>Envios organizados por destinatário confirmado.</p></div>
              </div><span className={styles.badge}>Identificação indisponível</span></div>
              <div className={styles.pixEmpty}>
                <span className={styles.emptyIcon}><Users size={28} aria-hidden="true" /></span>
                <div><h3>Os destinatários ainda não estão identificados</h3>
                  <p>O extrato analisado não fornece destinatários PIX estruturados e confirmados. Por isso, o ranking, a quantidade por destinatário e os detalhes individuais não estão disponíveis.</p>
                  <a className={styles.textLink} href="#transactions-heading" onClick={() => { clearFilters(); setPayment("PIX"); }}>Consultar os PIX no extrato completo <ArrowRight size={16} aria-hidden="true" /></a>
                </div>
              </div>
            </Panel>

            <Panel id="cashflow-heading">
              <div className={styles.panelHeading}><div className={styles.titleGroup}>
                <span className={styles.sectionIcon}><ChartColumn size={21} aria-hidden="true" /></span>
                <div><h2 id="cashflow-heading">Entradas e saídas por semana</h2><p>De segunda a domingo</p></div>
              </div>
                <div className={styles.legend}><span><i className={styles.incomeSwatch} />Entradas</span><span><i className={styles.outflowSwatch} />Saídas</span></div>
              </div>
              {weeks.length === 0 ? (
                <div className={styles.empty}><ChartColumn size={25} aria-hidden="true" /><h3>Sem movimentações para exibir</h3><p>O fluxo semanal aparecerá quando houver transações reconhecidas.</p></div>
              ) : (
                <>
                  <div className={styles.chartScroll} tabIndex={0} role="region" aria-label="Gráfico semanal de entradas e saídas. Role horizontalmente para ver todas as semanas.">
                    <div className={styles.weekChart}>
                      {weeks.map(week => (
                        <div key={week.start} className={styles.week}>
                          <div className={styles.weekBars} aria-hidden="true">
                            <div className={styles.weekBarColumn}><span>{currency(week.income)}</span><div className={styles.incomeBar} style={{ height: `${week.income / weeklyMax * 140}px` }} /></div>
                            <div className={styles.weekBarColumn}><span>{currency(week.outflow)}</span><div className={styles.outflowBar} style={{ height: `${week.outflow / weeklyMax * 140}px` }} /></div>
                          </div>
                          <p className={styles.weekLabel}>{dateLabel(week.start, true)} – {dateLabel(week.end, true)}<span>{week.start.slice(0, 4)}{week.start.slice(0, 4) !== week.end.slice(0, 4) ? ` / ${week.end.slice(0, 4)}` : ""}</span></p>
                          <span className={styles.srOnly}>Entradas: {currency(week.income)}. Saídas: {currency(week.outflow)}.</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </>
              )}
            </Panel>

            <Panel id="transactions-heading">
              <div className={styles.panelHeading}><div className={styles.titleGroup}>
                <span className={styles.sectionIcon}><FileText size={21} aria-hidden="true" /></span>
                <div><h2 id="transactions-heading">Extrato completo</h2><p>Explore cada movimentação, da mais recente à mais antiga.</p></div>
              </div><a href="#summary-heading" className={styles.textLink}>Voltar ao resumo ↑</a></div>

              <form className={styles.filters} onSubmit={event => event.preventDefault()} aria-label="Filtros do extrato">
                <label className={styles.searchField}><span>Buscar por descrição</span><span className={styles.searchInput}><Search size={18} aria-hidden="true" /><input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Busque uma movimentação" /></span></label>
                <label><span>De</span><input type="date" value={startDate} max={endDate || undefined} onChange={event => setStartDate(event.target.value)} aria-invalid={invalidPeriod} aria-describedby={invalidPeriod ? "period-error" : undefined} /></label>
                <label><span>Até</span><input type="date" value={endDate} min={startDate || undefined} onChange={event => setEndDate(event.target.value)} aria-invalid={invalidPeriod} aria-describedby={invalidPeriod ? "period-error" : undefined} /></label>
                <label><span>Categoria</span><select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas as categorias</option>{Object.entries(categoryLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
                <label><span>Tipo</span><select value={type} onChange={event => setType(event.target.value)}><option value="">Entradas e saídas</option><option value="INCOME">Entradas</option><option value="EXPENSE">Saídas</option></select></label>
                <label><span>Meio de pagamento</span><select value={payment} onChange={event => setPayment(event.target.value)}><option value="">Todos os meios</option>{Object.entries(paymentLabels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
                <button type="button" className={styles.clearButton} disabled={!hasFilters} onClick={clearFilters}>Limpar filtros</button>
              </form>
              {invalidPeriod && <p id="period-error" className={styles.error} role="alert">A data inicial deve ser anterior ou igual à data final.</p>}
              <div className={styles.resultLine}><p role="status" aria-live="polite" aria-atomic="true">{filtered.length.toLocaleString("pt-BR")} de {analysis.transactions.length.toLocaleString("pt-BR")} transações</p><span>Os filtros se aplicam somente ao extrato completo.</span></div>

              {grouped.length === 0 ? (
                <div className={styles.empty}><Search size={25} aria-hidden="true" /><h3>{hasFilters ? "Nenhuma movimentação encontrada" : "Nenhuma transação reconhecida"}</h3><p>{hasFilters ? "Ajuste ou limpe os filtros para consultar outras movimentações." : "Não foram reconhecidas transações neste extrato."}</p>{hasFilters && <button type="button" onClick={clearFilters} className={styles.textButton}>Limpar filtros</button>}</div>
              ) : (
                <div className={styles.dateGroups}>
                  {grouped.map(([date, transactions]) => (
                    <section key={date} className={styles.dateGroup} aria-labelledby={`day-${date}`}>
                      <div className={styles.dateHeading}><h3 id={`day-${date}`}><time dateTime={date}>{dateLabel(date)}</time></h3><span>{transactions.length} {transactions.length === 1 ? "movimentação" : "movimentações"}</span></div>
                      <ul className={styles.transactionList}>
                        {transactions.map((transaction, index) => (
                          <li key={`${date}-${index}`} className={styles.transaction}>
                            <span className={`${styles.transactionIcon} ${transaction.type === "INCOME" ? styles.received : styles.sent}`}>
                              {transaction.type === "INCOME" ? <ArrowDownLeft size={19} aria-hidden="true" /> : <ArrowUpRight size={19} aria-hidden="true" />}
                            </span>
                            <div className={styles.transactionDescription}><strong>{transaction.description}</strong><div className={styles.transactionMeta}><span>{categoryLabels[transaction.category] ?? transaction.category}</span><span>{paymentLabels[transaction.paymentMethod] ?? "Não identificado"}</span>{transaction.requiresReview && <span className={styles.review}>Requer revisão</span>}</div></div>
                            <div className={styles.transactionAmount}><strong className={transaction.type === "INCOME" ? styles.positive : undefined}>{transaction.amount > 0 ? "+ " : ""}{currency(transaction.amount)}</strong><span>{transaction.type === "INCOME" ? "Entrada" : "Saída"}</span></div>
                          </li>
                        ))}
                      </ul>
                    </section>
                  ))}
                </div>
              )}
            </Panel>
            <div className={styles.bottomNote}><span>Mais clareza para suas próximas escolhas.</span><Link to="/analisar" className={styles.textLink}>Analisar outro extrato <ArrowRight size={16} aria-hidden="true" /></Link></div>
          </>
        )}
        <Link to="/" className={styles.backLink}><ArrowLeft size={16} aria-hidden="true" /> Voltar ao início</Link>
      </motion.main>
    </div>
  );
}

import { motion, useReducedMotion } from "framer-motion";
import { DollarSign, ChartPie, CreditCard } from "lucide-react";
import styles from "./DashboardPreview.module.css";

// Illustrative data only. The original dashboard-complete.svg remains the visual reference.
const recipients = [
  { name: "Kethelyn G.", width: "91.8%" },
  { name: "Vinícius K.", width: "70.3%" },
  { name: "Gabriel O.", width: "55.3%" },
  { name: "Shopee Brasil", width: "83.1%" },
  { name: "iFood", width: "40.1%" },
];

const categories = [
  { name: "Alimentação", percentage: 45, className: styles.food },
  { name: "Transporte", percentage: 25, className: styles.transport },
  { name: "Compras", percentage: 18, className: styles.shopping },
  { name: "Outros", percentage: 12, className: styles.other },
];

export default function DashboardPreview() {
  const reducedMotion = useReducedMotion();

  return (
    <section className={styles.section} aria-label="Prévia ilustrativa do dashboard">
      <figure className={styles.container}>
        <motion.div className={styles.stage}
          initial={reducedMotion ? false : { opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-60px" }}
          transition={{ duration: reducedMotion ? 0 : 0.8, ease: "easeOut" }}>

          <article className={`${styles.card} ${styles.recipients}`} aria-labelledby="preview-recipients">
            <div className={styles.cardHeading}>
              <h3 id="preview-recipients">Saiba para quem seu<br />dinheiro está indo</h3>
              <span className={styles.icon}><DollarSign aria-hidden="true" /></span>
            </div>
            <ul className={styles.recipientList}>
              {recipients.map(recipient => (
                <li key={recipient.name}>
                  <span>{recipient.name}</span>
                  <div className={styles.track} aria-hidden="true"><div style={{ width: recipient.width }} /></div>
                </li>
              ))}
            </ul>
          </article>

          <article className={`${styles.card} ${styles.categories}`} aria-labelledby="preview-categories">
            <div className={styles.cardHeading}>
              <h3 id="preview-categories">Gastos por<br />categoria</h3>
              <span className={styles.icon}><ChartPie aria-hidden="true" /></span>
            </div>
            <div className={styles.categoryContent}>
              <div className={styles.donut} aria-hidden="true" />
              <ul className={styles.legend}>
                {categories.map(category => (
                  <li key={category.name}>
                    <i className={category.className} aria-hidden="true" />
                    <span>{category.name}<span className={styles.srOnly}>: {category.percentage}%</span></span>
                  </li>
                ))}
              </ul>
            </div>
          </article>

          <article className={`${styles.card} ${styles.summary}`} aria-labelledby="preview-summary">
            <div className={styles.cardHeading}>
              <h3 id="preview-summary">Resumo financeiro</h3>
              <span className={styles.icon}><CreditCard aria-hidden="true" /></span>
            </div>
            <div className={styles.summaryContent}>
              <div className={styles.balance}><span>Saldo do período</span><strong>R$ 5.000,00</strong></div>
              <dl className={styles.totals}>
                <div><dt>Entradas</dt><dd className={styles.positive}>+ R$ 8.000,00</dd></div>
                <div><dt>Saídas</dt><dd className={styles.negative}>− R$ 3.000,00</dd></div>
              </dl>
            </div>
          </article>
        </motion.div>
        <figcaption className={styles.caption}>Prévia ilustrativa · Dados demonstrativos</figcaption>
      </figure>
    </section>
  );
}

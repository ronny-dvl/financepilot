import { motion } from "framer-motion";
import styles from "./WhyFinancePilot.module.css";

const REASONS = [
  { number: "1.", text: "Decisões mais inteligentes" },
  { number: "2.", text: "Economize seu tempo" },
  { number: "3.", text: "Mais controle financeiro" },
  { number: "4.", text: "Clareza de verdade" },
];

export default function WhyFinancePilot() {
  return (
    <section id="porque" className={styles.section} aria-labelledby="why-title">
      <div className={styles.inner}>
        <motion.div
          className={styles.header}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, ease: "easeOut" }}
        >
          <h2 id="why-title" className={styles.title}>
            Por que usar o FinancePilot?
          </h2>
          <p className={styles.subtitle}>
            <span className={styles.orangeText}>Menos</span> tempo analisando.{" "}
            <span className={styles.orangeText}>Mais</span> tempo vivendo.
          </p>
        </motion.div>

        <motion.div
          className={styles.grid}
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, delay: 0.1, ease: "easeOut" }}
        >
          {REASONS.map((reason) => (
            <div key={reason.number} className={styles.item}>
              <span className={styles.itemNumber}>{reason.number}</span>
              <p className={styles.itemText}>{reason.text}</p>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
}

import { motion } from "framer-motion";
import styles from "./DashboardPreview.module.css";
import dashboardSvg from "../../assets/dashboard-preview/dashboard-complete.svg";

export default function DashboardPreview() {
  return (
    <section className={styles.section} aria-label="Prévia do Dashboard">
      <div className={styles.container}>
        <motion.div
          className={styles.stage}
          initial={{ opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-60px" }}
          transition={{ duration: 0.8, ease: "easeOut" }}
        >
          <img
            src={dashboardSvg}
            alt="Visão geral do painel financeiro com gráficos de categorias, despesas e resumo"
            className={styles.dashboardImage}
            draggable={false}
          />

          {/* Efeito de blur progressivo inferior */}
          <div className={styles.blurOverlay} aria-hidden="true">
            <div className={styles.blurLayer1} />
            <div className={styles.blurLayer2} />
            <div className={styles.blurLayer3} />
            <div className={styles.blurLayer4} />
            <div className={styles.blurLayer5} />
            <div className={styles.gradientFade} />
          </div>
        </motion.div>
      </div>

      {/* Linha divisória e sombra suave de transição para a próxima seção */}
      <div className={styles.transitionDivider} aria-hidden="true" />
      <div className={styles.transitionSpacer} aria-hidden="true" />
    </section>
  );
}

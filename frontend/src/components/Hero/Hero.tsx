
import { motion, useReducedMotion } from "framer-motion";
import styles from "./Hero.module.css";
import AnimatedBot from "../AnimatedBot/AnimatedBot";

type Props = {
  onCtaClick: () => void;
};

export default function Hero({ onCtaClick }: Props) {
  const reducedMotion = useReducedMotion();
  return (
    <section className={styles.hero}>
      <motion.div
        className={styles.text}
        initial={reducedMotion ? false : { opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: 0.7,
          ease: "easeOut",
        }}
      >
        <h1>
          <span className={styles.titleLine}>Pare de <em>quebrar a cabeça</em></span>
          <br />
          <span className={styles.titleLine}>com suas finanças.</span>
        </h1>

        <p>
          Transforme seu extrato bancário em gráficos,
          análises e insights sobre seus gastos
        </p>

        <button
          className={styles.cta}
          onClick={onCtaClick}
        >
          Analisar meu extrato
        </button>
      </motion.div>

      <div className={styles.visual}>
        {/* Bot superior esquerdo */}
        <motion.div
          className={`${styles.bot} ${styles.botBack}`}
          animate={reducedMotion ? { y: 0, rotate: -2 } : {
            y: [0, 10, 0],
            rotate: [-2, 0, -2],
          }}
          transition={{
            duration: 6,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        >
          <AnimatedBot
            size={318}
            heroSelector={`.${styles.hero}`}
          />
        </motion.div>

        {/* Bot inferior direito */}
        <motion.div
          className={`${styles.bot} ${styles.botFront}`}
          animate={reducedMotion ? { y: 0, rotate: 7 } : {
            y: [0, -14, 0],
            rotate: [10, 7, 10],
          }}
          transition={{
            duration: 5,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        >
          <AnimatedBot
            size={318}
            heroSelector={`.${styles.hero}`}
          />
        </motion.div>
      </div>
    </section>
  );
}

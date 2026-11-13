
import { motion } from "framer-motion";
import styles from "./Hero.module.css";
import AnimatedBot from "../AnimatedBot/AnimatedBot";

type Props = {
  onCtaClick: () => void;
};

export default function Hero({ onCtaClick }: Props) {
  return (
    <section className={styles.hero}>
      <motion.div
        className={styles.text}
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: 0.7,
          ease: "easeOut",
        }}
      >
        <h1>
          Pare de <em>quebrar a cabeça</em>
          <br />
          com suas finanças.
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
          animate={{
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
          animate={{
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

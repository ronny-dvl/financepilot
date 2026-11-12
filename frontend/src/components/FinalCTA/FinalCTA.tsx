import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import styles from "./FinalCTA.module.css";

import char1 from "../../assets/characters/char-1.svg";
import char3 from "../../assets/characters/char-3.svg";
import char5 from "../../assets/characters/char-5.svg";

type Props = {
  onCtaClick?: () => void;
};

export default function FinalCTA({ onCtaClick }: Props) {
  const navigate = useNavigate();

  const handleCta = () => {
    if (onCtaClick) {
      onCtaClick();
    } else {
      navigate("/analisar");
    }
  };

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <section className={styles.section} aria-label="Chamada para ação">
      <div className={styles.container}>
        <motion.div
          className={styles.card}
          initial={{ opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          {/* Fundo animado com iluminação quente adaptada do 3º card de Steps */}
          <div className={styles.ambientGlow} aria-hidden="true" />

          <div className={styles.content}>
            <div className={styles.textColumn}>
              <h2 className={styles.title}>
                O mês acabou. O dinheiro também.
                <br />
                Mas foi pra onde?
              </h2>
              <p className={styles.description}>
                Entre um Pix, uma compra e outra, fica difícil acompanhar tudo. Deixe o FinancePilot organizar essa história.
              </p>
              <div className={styles.actions}>
                <button
                  type="button"
                  className={styles.primaryBtn}
                  onClick={handleCta}
                >
                  Analisar meu extrato <span className={styles.arrow} aria-hidden="true">→</span>
                </button>
                <button
                  type="button"
                  className={styles.secondaryBtn}
                  onClick={scrollToTop}
                >
                  Voltar ao topo
                </button>
              </div>
            </div>

            <div className={styles.botsColumn} aria-hidden="true">
              <div className={styles.botsCluster}>
                <div className={styles.botsGlow} />

                {/* Bot 1: char-1.svg (Superior Esquerdo) */}
                <motion.div
                  className={`${styles.botWrapper} ${styles.bot1}`}
                  animate={{
                    y: [-4, 5, -4],
                    x: [-2, 3, -2],
                    rotate: [-5, 0, -5],
                  }}
                  transition={{
                    duration: 4.8,
                    repeat: Infinity,
                    ease: "easeInOut",
                  }}
                >
                  <img
                    src={char1}
                    alt=""
                    className={styles.botImg}
                    draggable={false}
                  />
                </motion.div>

                {/* Bot 2: char-5.svg (Superior Direito) */}
                <motion.div
                  className={`${styles.botWrapper} ${styles.bot2}`}
                  animate={{
                    y: [4, -5, 4],
                    x: [3, -2, 3],
                    rotate: [12, 16, 12],
                  }}
                  transition={{
                    duration: 5.4,
                    repeat: Infinity,
                    ease: "easeInOut",
                    delay: 0.4,
                  }}
                >
                  <img
                    src={char5}
                    alt=""
                    className={styles.botImg}
                    draggable={false}
                  />
                </motion.div>

                {/* Bot 3: char-3.svg (Inferior) */}
                <motion.div
                  className={`${styles.botWrapper} ${styles.bot3}`}
                  animate={{
                    y: [-5, 4, -5],
                    x: [2, -3, 2],
                    rotate: [-11, -7, -11],
                  }}
                  transition={{
                    duration: 6.0,
                    repeat: Infinity,
                    ease: "easeInOut",
                    delay: 0.8,
                  }}
                >
                  <img
                    src={char3}
                    alt=""
                    className={styles.botImg}
                    draggable={false}
                  />
                </motion.div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

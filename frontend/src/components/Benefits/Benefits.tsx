import { motion } from "framer-motion";
import styles from "./Benefits.module.css";
import {
  CHARACTERS_LAYOUT,
  getCharacterMotion,
  cardsContainerVariants,
  cardItemVariants,
} from "./benefitsAnimation";

import char1 from "../../assets/characters/char-1.svg";
import char2 from "../../assets/characters/char-2.svg";
import char3 from "../../assets/characters/char-3.svg";
import char4 from "../../assets/characters/char-4.svg";
import char5 from "../../assets/characters/char-5.svg";
import char6 from "../../assets/characters/char-6.svg";

const CHAR_IMAGES: Record<string, string> = {
  "char-1": char1,
  "char-2": char2,
  "char-3": char3,
  "char-4": char4,
  "char-5": char5,
  "char-6": char6,
};

const BENEFIT_CARDS = [
  {
    title: "Quais gastos se repetem?",
    description: "Identifique pagamentos frequentes e hábitos que impactam seu orçamento.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21.5 2v6h-6" />
        <path d="M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.19" />
      </svg>
    ),
  },
  {
    title: "Onde posso economizar?",
    description: "Encontre oportunidades para repensar seus gastos e tomar decisões mais conscientes.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
      </svg>
    ),
  },
  {
    title: "Como meus gastos se distribuem?",
    description: "Explore gráficos que tornam suas movimentações mais fáceis de entender.",
    icon: (
      <svg viewBox="0 0 24 24" fill="none" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 12h5l3-7 4 14 3-7h5" />
      </svg>
    ),
  },
];

export default function Benefits() {
  return (
    <section id="beneficios" className={styles.section}>
      <div className={styles.inner}>
        <motion.h2
          className={styles.title}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, ease: "easeOut" }}
        >
          Seu dinheiro tem uma historia.{" "}
          <span className={styles.highlight}>Descubra cada detalhe</span>
        </motion.h2>

        <div className={styles.content}>
          {/* Anel orbital dos 6 personagens com a animação Figma Motion */}
          <div className={styles.orbitArea} aria-hidden>
            {CHARACTERS_LAYOUT.map((char) => {
              const motionProps = getCharacterMotion(char);
              return (
                <motion.div
                  key={char.id}
                  className={styles.charWrapper}
                  style={{ zIndex: char.zIndex }}
                  animate={motionProps.animate}
                  transition={motionProps.transition}
                >
                  <img
                    src={CHAR_IMAGES[char.id]}
                    alt=""
                    className={styles.charImg}
                    draggable={false}
                  />
                </motion.div>
              );
            })}
          </div>

          {/* Cards de benefícios da coluna direita */}
          <motion.div
            className={styles.cardsList}
            variants={cardsContainerVariants}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: "-60px" }}
          >
            {BENEFIT_CARDS.map((card) => (
              <motion.article
                key={card.title}
                className={styles.card}
                variants={cardItemVariants}
              >
                <div className={styles.cardText}>
                  <h3 className={styles.cardTitle}>{card.title}</h3>
                  <p className={styles.cardDescription}>{card.description}</p>
                </div>
                <div className={styles.badge} aria-hidden>
                  {card.icon}
                </div>
              </motion.article>
            ))}
          </motion.div>
        </div>
      </div>
    </section>
  );
}

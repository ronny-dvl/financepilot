import styles from "./Steps.module.css";
import arrow from "../../assets/step-arrow.svg";
import trend from "../../assets/step-trend.svg";
import grid from "../../assets/step-grid.svg";

type Props = { onCtaClick: () => void };

const cards = [
  { icon: arrow, w: 68, title: ["Envie seu", "extrato"], text: "Selecione seu extrato bancário em PDF." },
  { icon: trend, w: 79, title: ["Análise", "automática"], text: "O FinancePilot identifica suas movimentações." },
  { icon: grid, w: 59, title: ["Explore seus", "gastos"], text: "Visualize gráficos, resumos e destinatários.", dark: true },
];

export default function Steps({ onCtaClick }: Props) {
  return (
    <section id="como-funciona" className={styles.section}>
      <h2 className={styles.title}>
        São apenas <span>3</span> passos.
      </h2>

      <div className={styles.grid}>
        {cards.map((c) => (
          <article key={c.title.join(" ")} className={`${styles.card} ${c.dark ? styles.dark : ""}`}>
            <div className={styles.inner}>
              {/* largura do ícone em cqw = largura do asset / 415 */}
              <img src={c.icon} alt="" className={styles.icon} style={{ width: `${(c.w / 415) * 100}cqw` }} />
              <div>
                <h3>
                  {c.title[0]}
                  <br />
                  {c.title[1]}
                </h3>
                <p>{c.text}</p>
              </div>
            </div>
          </article>
        ))}
      </div>

      <button className={styles.cta} onClick={onCtaClick}>
        Analisar meu extrato
      </button>
    </section>
  );
}

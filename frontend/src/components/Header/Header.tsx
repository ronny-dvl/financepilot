import { useEffect, useState } from "react";
import styles from "./Header.module.css";
import logo from "../../assets/logo.svg";

type Props = { onCtaClick: () => void };

export default function Header({ onCtaClick }: Props) {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header className={`${styles.header} ${scrolled ? styles.scrolled : ""}`}>
      <div className={styles.bar}>
        <a href="/" className={styles.logo} aria-label="FinancePilot">
          <img src={logo} alt="FinancePilot" />
        </a>

        <nav className={styles.nav}>
          <a href="#como-funciona">Como funciona?</a>
          <a href="#beneficios">Benefícios</a>
          <a href="#porque">Por que usar o FinancePilot?</a>
        </nav>

        <button className={styles.cta} onClick={onCtaClick}>
          Analisar meu extrato
        </button>
      </div>
    </header>
  );
}

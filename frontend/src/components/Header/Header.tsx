import { useEffect, useLayoutEffect, useRef, useState } from "react";
import styles from "./Header.module.css";
import logo from "../../assets/logo.svg";
import ThemeSelector from "../ThemeSelector/ThemeSelector";

type Props = { onCtaClick: () => void };

export default function Header({ onCtaClick }: Props) {
  const [scrolled, setScrolled] = useState(false);
  const headerRef = useRef<HTMLElement>(null);

  useLayoutEffect(() => {
    const header = headerRef.current;
    if (!header) return;
    const root = document.documentElement;
    const previous = root.style.getPropertyValue("--header-offset");
    const update = () => root.style.setProperty("--header-offset", `${header.getBoundingClientRect().height}px`);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(header);
    return () => {
      observer.disconnect();
      if (previous) root.style.setProperty("--header-offset", previous);
      else root.style.removeProperty("--header-offset");
    };
  }, []);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header ref={headerRef} className={`${styles.header} ${scrolled ? styles.scrolled : ""}`}>
      <div className={styles.bar}>
        <a href="/" className={styles.logo} aria-label="FinancePilot">
          <img src={logo} alt="FinancePilot" />
        </a>

        <nav className={styles.nav}>
          <a href="#como-funciona">Como funciona?</a>
          <a href="#beneficios">Benefícios</a>
          <a href="#porque">Por que usar o FinancePilot?</a>
        </nav>

        <ThemeSelector />
        <button className={styles.cta} onClick={onCtaClick}>
          <span className={styles.desktopLabel}>Analisar meu extrato</span>
          <span className={styles.mobileLabel}>Analisar</span>
        </button>
      </div>
    </header>
  );
}

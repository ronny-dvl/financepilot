import { useState } from "react";
import Header from "../components/Header/Header";
import Hero from "../components/Hero/Hero";
import Steps from "../components/Steps/Steps";
import Benefits from "../components/Benefits/Benefits";
import AuthOverlay from "../components/AuthOverlay";
import styles from "./Landing.module.css";

export default function Landing() {
  const [open, setOpen] = useState(false);
  const abrir = () => setOpen(true);

  return (
    <>
      <div className={styles.heroWrap}>
        <div className={styles.inner}>
          <Header onCtaClick={abrir} />
          <Hero onCtaClick={abrir} />
        </div>
      </div>

      <Steps onCtaClick={abrir} />
      <Benefits />
      {/* próximas seções entram aqui (ids: porque) */}

      <AuthOverlay open={open} onClose={() => setOpen(false)} />
    </>
  );
}

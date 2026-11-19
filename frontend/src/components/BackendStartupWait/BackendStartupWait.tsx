import { LoaderCircle, Server } from "lucide-react";
import { motion, useReducedMotion } from "framer-motion";
import styles from "./BackendStartupWait.module.css";

type Props = { failed: boolean; onRetry: () => void; onCancel: () => void };

export default function BackendStartupWait({ failed, onRetry, onCancel }: Props) {
  const reducedMotion = useReducedMotion();
  return (
    <div className={styles.wrapper}>
      <motion.section className={styles.panel} aria-labelledby="startup-title" aria-describedby="startup-description"
        initial={reducedMotion ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: reducedMotion ? 0 : 0.2 }}>
        <span className={styles.icon}><Server size={30} strokeWidth={1.8} aria-hidden="true" /></span>
        <div className={styles.message} role={failed ? "alert" : "status"} aria-live="polite" aria-atomic="true">
          <h1 id="startup-title">{failed ? "Não foi possível conectar" : "Preparando seu ambiente..."}</h1>
          <p id="startup-description">{failed
            ? "O servidor ainda não respondeu. Você pode tentar novamente ou voltar para continuar depois."
            : "Estamos iniciando o servidor para analisar seu extrato com segurança. Isso pode levar alguns instantes."}</p>
        </div>
        {!failed && <>
          <div className={styles.connecting}><LoaderCircle className={styles.spinner} size={18} aria-hidden="true" /><span>Conectando ao servidor...</span></div>
          <div className={styles.track} role="progressbar" aria-label="Conexão com o servidor" aria-valuetext="Aguardando o servidor ficar disponível" aria-busy="true"><span className={styles.segment} /></div>
        </>}
        <p className={styles.footer}>O primeiro acesso pode demorar um pouco mais.</p>
        {failed && <button type="button" className={styles.retry} onClick={onRetry}>Tentar novamente</button>}
      </motion.section>
      <button type="button" className={styles.cancel} onClick={onCancel}>{failed ? "Voltar" : "Cancelar espera"}</button>
    </div>
  );
}

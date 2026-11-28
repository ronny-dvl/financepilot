import { useEffect, useRef } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { FileSearch, ScanLine, ChartNoAxesCombined, CircleCheck } from "lucide-react";
import ThemeSelector from "../ThemeSelector/ThemeSelector";
import logo from "../../assets/logo.svg";
import styles from "./AnalysisTransition.module.css";

export type AnalysisStep = 0 | 1 | 2 | 3;
const steps = [
  { icon: FileSearch, title: "Analisando seu extrato", subtitle: "Extraindo as informações do seu extrato." },
  { icon: ScanLine, title: "Organizando suas movimentações", subtitle: "Identificando entradas, saídas e categorias." },
  { icon: ChartNoAxesCombined, title: "Preparando seu dashboard", subtitle: "Organizando suas informações financeiras." },
  { icon: CircleCheck, title: "Bem-vindo ao dashboard", subtitle: "Sua análise está pronta." },
];

type Props = {
  step: AnalysisStep;
  progress: number;
  waitingForServer: boolean;
  onCancel: () => void;
};

export default function AnalysisTransition({ step, progress, waitingForServer, onCancel }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const reducedMotion = useReducedMotion();
  const { icon: Icon, title, subtitle } = steps[step];

  useEffect(() => {
    const dialog = dialogRef.current;
    const previouslyFocused = document.activeElement as HTMLElement | null;
    dialog?.showModal();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      dialog?.close();
      document.body.style.overflow = overflow;
      previouslyFocused?.focus();
    };
  }, []);

  return (
    <dialog ref={dialogRef} className={styles.screen} aria-labelledby="analysis-title"
      aria-describedby="analysis-subtitle" onCancel={event => { event.preventDefault(); if (progress < 100) onCancel(); }}>
      <div className={styles.topbar}><img src={logo} alt="FinancePilot" /><ThemeSelector /></div>
      <div className={styles.center}>
        <div className={styles.messages} role="status" aria-live="polite" aria-atomic="true">
          <AnimatePresence mode="wait" initial={false}>
            <motion.div key={step}
              initial={reducedMotion ? false : { opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={reducedMotion ? undefined : { opacity: 0, y: -6 }}
              transition={{ duration: reducedMotion ? 0 : 0.18 }}>
              <motion.div className={styles.icon} aria-hidden="true"
                animate={step === 1 && !reducedMotion ? { opacity: [0.65, 1, 0.65] } : { opacity: 1 }}
                transition={step === 1 && !reducedMotion ? { duration: 1.8, repeat: Infinity } : { duration: 0 }}>
                <Icon size={38} strokeWidth={1.6} />
              </motion.div>
              <h1 id="analysis-title">{title}</h1>
              <p id="analysis-subtitle">{subtitle}</p>
            </motion.div>
          </AnimatePresence>
        </div>
        <div className={styles.progress}>
          <div className={styles.percentage} aria-hidden="true">{Math.floor(progress)}%</div>
          <div className={styles.track} role="progressbar" aria-label="Progresso estimado da análise"
            aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.floor(progress)}>
            <motion.div className={styles.fill} initial={false} animate={{ width: `${progress}%` }}
              transition={{ duration: reducedMotion ? 0 : 0.1, ease: "linear" }} />
          </div>
        </div>
        <div className={styles.serverMessage} role="status" aria-live="polite">
          <AnimatePresence initial={false}>
            {waitingForServer && <motion.p key="server" initial={{ opacity: 0 }} animate={{ opacity: 1 }}
              exit={{ opacity: 0 }} transition={{ duration: reducedMotion ? 0 : 0.2 }}>
              Estamos iniciando o servidor...
            </motion.p>}
          </AnimatePresence>
        </div>
        <div className={styles.actions}>
          {progress < 100
            ? <button type="button" onClick={onCancel} className={styles.secondary}>Cancelar análise</button>
            : <span className={styles.completed} role="status">Análise concluída</span>}
        </div>
      </div>
    </dialog>
  );
}

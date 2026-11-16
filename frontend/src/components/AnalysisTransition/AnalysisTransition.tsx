import { useEffect, useRef } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { FileText, ScanLine, CheckCircle2, LayoutDashboard } from "lucide-react";
import ThemeSelector from "../ThemeSelector/ThemeSelector";
import logo from "../../assets/logo.svg";
import styles from "./AnalysisTransition.module.css";

export type AnalysisStep = 0 | 1 | 2 | 3;
const steps = [
  { icon: FileText, title: "Extrato enviado", subtitle: "Seu PDF está pronto para análise." },
  { icon: ScanLine, title: "Organizando suas movimentações", subtitle: "Identificando entradas, saídas e categorias." },
  { icon: CheckCircle2, title: "Análise concluída", subtitle: "Seu painel financeiro está pronto." },
  { icon: LayoutDashboard, title: "Bem-vindo ao seu dashboard", subtitle: "Seus dados financeiros, organizados para você." },
];

type Props = {
  step: AnalysisStep;
  onBack: () => void;
};

export default function AnalysisTransition({ step, onBack }: Props) {
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
      aria-describedby="analysis-subtitle" onCancel={event => { event.preventDefault(); onBack(); }}>
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
        <ol className={styles.steps} aria-label="Etapas da análise">
          {steps.map((item, index) => (
            <li key={item.title} aria-current={index === step ? "step" : undefined}
              className={index <= step ? styles.active : undefined}>
              <span aria-hidden="true" />
              <span className={styles.srOnly}>{item.title}{index < step ? ": concluída" : index === step ? ": etapa atual" : ": próxima etapa"}</span>
            </li>
          ))}
        </ol>
        <div className={styles.actions}>
          {step < 2 && <button type="button" onClick={onBack} className={styles.secondary}>{step === 1 ? "Cancelar análise" : "Voltar ao arquivo selecionado"}</button>}
        </div>
      </div>
    </dialog>
  );
}

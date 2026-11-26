
import { useEffect, useRef, useState, type FormEvent } from "react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { useLocation, useNavigate } from "react-router-dom";
import { ArrowLeft, ArrowRight, Eye, EyeOff, UserRound, Wallet, X } from "lucide-react";
import { useStatement } from "../contexts/StatementContext";
import { useBackendReadiness } from "../contexts/BackendReadinessContext";
import styles from "./AuthOverlay.module.css";

type Props = {
  open: boolean;
  onClose: () => void;
};

const emptyForm = { nome: "", sobrenome: "", email: "", senha: "" };
const validName = (value: string) => value.trim().length >= 2 && value.trim().length <= 80
  && /^[\p{L}\p{M}]+(?:[ '’\-][\p{L}\p{M}]+)*$/u.test(value.trim());

export default function AuthOverlay({ open, onClose }: Props) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const reduceMotion = useReducedMotion();
  const { setGuestFirstName } = useStatement();
  const { ensureReady, cancel } = useBackendReadiness();
  const continuingToUpload = useRef(false);

  useEffect(() => {
    if (!open) return;
    continuingToUpload.current = false;
    ensureReady();
    return () => { if (!continuingToUpload.current) cancel(); };
  }, [open, ensureReady, cancel]);
  const [step, setStep] = useState<"choice" | "register" | "guest">("choice");
  const [guestError, setGuestError] = useState("");
  const modalRef = useRef<HTMLDivElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);

  const [form, setForm] = useState({
    nome: "",
    sobrenome: "",
    email: "",
    senha: "",
  });

  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (open) return;
    setStep("choice");
    setForm(emptyForm);
    setGuestError("");
    setShowPassword(false);
  }, [open]);

  useEffect(() => { if (open) titleRef.current?.focus(); }, [open, step]);

  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
      if (event.key !== "Tab") return;
      const focusable = modalRef.current?.querySelectorAll<HTMLElement>(
        'button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]'
      );
      if (!focusable?.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && (document.activeElement === first || document.activeElement === titleRef.current)) {
        event.preventDefault(); last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault(); first.focus();
      }
    };

    window.addEventListener("keydown", onKeyDown);

    return () => {
      window.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, onClose]);

  const updateField =
    (field: keyof typeof form) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setForm((previous) => ({
        ...previous,
        [field]: event.target.value,
      }));
    };

  const criarConta = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    // TODO: Integrar POST /api/auth/register.
    // Não simular cadastro enquanto a API não estiver pronta.
  };

  const changeStep = (next: "choice" | "register" | "guest") => {
    setForm(emptyForm);
    setGuestError("");
    setShowPassword(false);
    setStep(next);
  };

  const entrarComoConvidado = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nome = form.nome.trim().replace(/\s+/g, " ");
    if (!validName(nome)) {
      setGuestError("Informe um nome válido, com pelo menos duas letras, sem números ou símbolos.");
      document.getElementById("guest-nome")?.focus();
      return;
    }
    // Only the first name is kept in React memory for the dashboard greeting.
    continuingToUpload.current = true;
    setGuestFirstName(nome.split(" ")[0]);
    setForm(emptyForm);
    if (pathname !== "/analisar") onClose();
    navigate("/analisar", { replace: pathname === "/analisar" });
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className={styles.overlay}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          onClick={onClose}
        >
          <motion.div
            ref={modalRef}
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="auth-title"
            initial={
              reduceMotion
                ? false
                : { opacity: 0, scale: 0.95, y: 20 }
            }
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={
              reduceMotion
                ? undefined
                : { opacity: 0, scale: 0.96, y: 12 }
            }
            transition={{
              type: "spring",
              stiffness: 320,
              damping: 28,
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <div className={styles.header}>
              <div className={styles.brand}>
                <div className={styles.brandIcon}>
                  <Wallet size={22} strokeWidth={1.9} />
                </div>

                <span>FinancePilot</span>
              </div>

              <button
                type="button"
                className={styles.closeButton}
                onClick={onClose}
                aria-label="Fechar formulário"
              >
                <X size={20} />
              </button>
            </div>

            {step !== "choice" && <button type="button" className={styles.back} onClick={() => changeStep("choice")}><ArrowLeft size={16} aria-hidden="true" /> Voltar</button>}

            <h2 ref={titleRef} tabIndex={-1} id="auth-title" className={styles.title}>
              {step === "choice" ? "Como deseja começar?" : step === "guest" ? "Como podemos chamar você?" : "Comece a entender suas finanças."}
            </h2>

            {step === "choice" && <div className={`${styles.form} ${styles.choiceButtons}`}>
              <button type="button" className={`${styles.primary} ${styles.choiceButton} ${styles.choicePrimary}`} onClick={() => changeStep("register")}>
                <span aria-hidden="true" />
                <span>Criar conta</span>
                <ArrowRight size={18} aria-hidden="true" />
              </button>
              <button type="button" className={`${styles.primary} ${styles.choiceButton} ${styles.secondaryChoice}`} onClick={() => changeStep("guest")}>
                <UserRound size={18} aria-hidden="true" />
                <span>Continuar como convidado</span>
                <ArrowRight size={18} aria-hidden="true" />
              </button>
            </div>}

            {step === "guest" && <form className={styles.form} onSubmit={entrarComoConvidado}>
              <div className={styles.field}><label htmlFor="guest-nome">Nome</label><input id="guest-nome" autoComplete="given-name" placeholder="Digite seu nome" value={form.nome} onChange={updateField("nome")} required minLength={2} maxLength={80} aria-invalid={!!guestError && !validName(form.nome.trim().replace(/\s+/g, " "))} aria-describedby={guestError ? "guest-error" : undefined} /></div>
              {guestError && <p id="guest-error" className={styles.error} role="alert">{guestError}</p>}
              <button type="submit" className={styles.primary}>Continuar para o upload <span aria-hidden="true">→</span></button>
            </form>}

            {step === "register" && <form className={styles.form} onSubmit={criarConta}>
              <p className={styles.notice}>O cadastro ainda não está disponível. Nenhum dado deste formulário será enviado.</p>
              <div className={styles.nameRow}>
                <div className={styles.field}>
                  <label htmlFor="auth-nome">Nome</label>
                  <input
                    id="auth-nome"
                    placeholder="Seu nome"
                    autoComplete="given-name"
                    value={form.nome}
                    onChange={updateField("nome")}
                    required
                  />
                </div>

                <div className={styles.field}>
                  <label htmlFor="auth-sobrenome">Sobrenome</label>
                  <input
                    id="auth-sobrenome"
                    placeholder="Seu sobrenome"
                    autoComplete="family-name"
                    value={form.sobrenome}
                    onChange={updateField("sobrenome")}
                    required
                  />
                </div>
              </div>

              <div className={styles.field}>
                <label htmlFor="auth-email">E-mail</label>
                <input
                  id="auth-email"
                  type="email"
                  placeholder="seu@email.com"
                  autoComplete="email"
                  value={form.email}
                  onChange={updateField("email")}
                  required
                />
              </div>

              <div className={styles.field}>
                <label htmlFor="auth-senha">Senha</label>

                <div className={styles.passwordWrapper}>
                  <input
                    id="auth-senha"
                    type={showPassword ? "text" : "password"}
                    placeholder="Mínimo de 8 caracteres"
                    autoComplete="new-password"
                    minLength={8}
                    value={form.senha}
                    onChange={updateField("senha")}
                    required
                  />

                  <button
                    type="button"
                    className={styles.passwordToggle}
                    onClick={() =>
                      setShowPassword((previous) => !previous)
                    }
                    aria-label={
                      showPassword ? "Ocultar senha" : "Mostrar senha"
                    }
                    aria-pressed={showPassword}
                  >
                    {showPassword ? (
                      <EyeOff size={19} />
                    ) : (
                      <Eye size={19} />
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                className={styles.primary}
                disabled
                title="Cadastro disponível em breve"
              >
                Criar minha conta
                <span aria-hidden="true">→</span>
              </button>

              <div className={styles.guestRow}>
                <span>Quer conhecer primeiro?</span>

                <button
                  type="button"
                  className={styles.guest}
                  onClick={() => changeStep("guest")}
                >
                  Entrar como convidado
                </button>
              </div>

            </form>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

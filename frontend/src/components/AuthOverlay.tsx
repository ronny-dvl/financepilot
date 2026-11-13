
import { useEffect, useState, type FormEvent } from "react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Eye, EyeOff, Wallet, X } from "lucide-react";
import styles from "./AuthOverlay.module.css";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function AuthOverlay({ open, onClose }: Props) {
  const navigate = useNavigate();
  const reduceMotion = useReducedMotion();

  const [form, setForm] = useState({
    nome: "",
    sobrenome: "",
    email: "",
    senha: "",
  });

  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    window.addEventListener("keydown", onKeyDown);

    return () => {
      window.removeEventListener("keydown", onKeyDown);
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

  const entrarComoVisitante = () => {
    onClose();
    navigate("/analisar");
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

            <h2 id="auth-title" className={styles.title}>
              Comece a entender suas finanças.
            </h2>

            <form className={styles.form} onSubmit={criarConta}>
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
                  onClick={entrarComoVisitante}
                >
                  Continuar como visitante
                </button>
              </div>

              <p className={styles.notice}>
                O cadastro estará disponível em breve.
              </p>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

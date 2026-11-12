import { useEffect, useState, type FormEvent } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import styles from "./AuthOverlay.module.css";
import bot1 from "../assets/bot-1.svg";

type Props = { open: boolean; onClose: () => void };

export default function AuthOverlay({ open, onClose }: Props) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ nome: "", sobrenome: "", email: "", senha: "" });

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  const set = (campo: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [campo]: e.target.value });

  const criarConta = (e: FormEvent) => {
    e.preventDefault();
    // TODO: POST /api/auth/register com `form` quando o backend voltar
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
          onClick={onClose}
        >
          <motion.div
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-label="Criar conta"
            initial={{ scale: 0.92, y: 20 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.92, y: 20 }}
            onClick={(e) => e.stopPropagation()}
          >
            <img src={bot1} alt="" className={styles.badge} aria-hidden />

            <form className={styles.form} onSubmit={criarConta}>
              <input aria-label="Nome" placeholder="Nome" autoComplete="given-name"
                value={form.nome} onChange={set("nome")} required />
              <input aria-label="Sobrenome" placeholder="Sobrenome" autoComplete="family-name"
                value={form.sobrenome} onChange={set("sobrenome")} required />
              <input aria-label="Email" type="email" placeholder="Email" autoComplete="email"
                value={form.email} onChange={set("email")} required />
              <input aria-label="Senha" type="password" placeholder="Senha" autoComplete="new-password"
                value={form.senha} onChange={set("senha")} required minLength={6} />

              <button type="submit" className={styles.primary}>
                Criar conta <span aria-hidden>→</span>
              </button>
              <button type="button" className={styles.guest} onClick={() => navigate("/analisar")}>
                Entrar como visitante
              </button>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

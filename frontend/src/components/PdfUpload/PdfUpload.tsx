
import { useCallback, useEffect, useRef, useState } from "react";
import { useDropzone, type FileRejection } from "react-dropzone";
import {
  AnimatePresence,
  motion,
  useReducedMotion,
} from "framer-motion";
import {
  ArrowUpFromLine,
  CheckCircle2,
  FileText,
  X,
  AlertCircle,
} from "lucide-react";
import styles from "./PdfUpload.module.css";

const MAX_SIZE = 10 * 1024 * 1024;
const SUCTION_DURATION = 650;

type AnimationPhase = "idle" | "sucking" | "selected";

export default function PdfUpload() {
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState("");
  const [shake, setShake] = useState(0);
  const [phase, setPhase] = useState<AnimationPhase>("idle");

  const reduceMotion = useReducedMotion();

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const zoneRef = useRef<HTMLDivElement | null>(null);

  const [origin, setOrigin] = useState({ x: 0, y: -100 });

  const clearAnimation = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  useEffect(() => {
    return () => clearAnimation();
  }, [clearAnimation]);

  const selectFile = useCallback(
    (selected: File) => {
      clearAnimation();

      setFile(selected);
      setError("");

      if (reduceMotion) {
        setPhase("selected");
        return;
      }

      setPhase("sucking");

      timerRef.current = setTimeout(() => {
        setPhase("selected");
        timerRef.current = null;
      }, SUCTION_DURATION);
    },
    [clearAnimation, reduceMotion]
  );

  const onDrop = useCallback(
    (accepted: File[]) => {
      if (!accepted.length) return;
      selectFile(accepted[0]);
    },
    [selectFile]
  );

  const onDropRejected = useCallback(
    (rejections: FileRejection[]) => {
      const codes = rejections.flatMap(({ errors }) =>
        errors.map((error) => error.code)
      );

      if (codes.includes("file-too-large")) {
        setError("O arquivo deve ter no máximo 10 MB.");
      } else if (codes.includes("too-many-files")) {
        setError("Selecione apenas um PDF por vez.");
      } else {
        setError("Selecione um arquivo PDF válido.");
      }

      setShake((value) => value + 1);
    },
    []
  );

  const {
    getRootProps,
    getInputProps,
    isDragActive,
    open,
  } = useDropzone({
    accept: { "application/pdf": [".pdf"] },
    maxFiles: 1,
    multiple: false,
    maxSize: MAX_SIZE,
    noClick: true,
    noKeyboard: true,
    onDrop,
    onDropRejected,
  });

  const rootProps = getRootProps({
  className: styles.dropzone,
  onDrop: (event) => {
    const rect = event.currentTarget.getBoundingClientRect();

    const x = event.clientX - (rect.left + rect.width / 2);
    const y = event.clientY - (rect.top + rect.height / 2);

    setOrigin({ x, y });
  },
});

  const removeFile = () => {
    clearAnimation();
    setFile(null);
    setError("");
    setPhase("idle");
  };

  const isSucking = phase === "sucking";

  return (
    <div className={styles.container}>
      <motion.div
  {...rootProps}
  data-dragging={isDragActive || undefined}
  data-sucking={isSucking || undefined}
  animate={{
    scale: isDragActive && !reduceMotion ? 1.025 : 1,
  }}
  transition={{
    type: "spring",
    stiffness: 300,
    damping: 22,
  }}
>
        <input {...getInputProps()} />

        <AnimatePresence mode="wait">
          {phase === "idle" && (
            <motion.div
              key="empty"
              className={styles.emptyState}
              initial={reduceMotion ? false : { opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={
                reduceMotion
                  ? undefined
                  : { opacity: 0, scale: 0.92 }
              }
              transition={{ duration: 0.18 }}
            >
              <motion.div
                className={styles.iconBox}
                animate={{
                  y: isDragActive && !reduceMotion ? -10 : 0,
                  scale: isDragActive && !reduceMotion ? 1.12 : 1,
                }}
                transition={{
                  type: "spring",
                  stiffness: 280,
                  damping: 15,
                }}
              >
                <ArrowUpFromLine size={30} strokeWidth={1.8} />
              </motion.div>

              <h2>
                {isDragActive
                  ? "Solte seu PDF aqui"
                  : "Arraste seu extrato bancário"}
              </h2>

              <p>Ou selecione um arquivo do seu dispositivo</p>

              <button
                type="button"
                className={styles.selectButton}
                onClick={open}
              >
                Selecionar PDF
              </button>

              <span className={styles.helper}>
                Arquivos PDF de até 10 MB
              </span>
            </motion.div>
          )}

          {phase === "selected" && file && (
            <motion.div
              key="selected"
              className={styles.selectedState}
              initial={
                reduceMotion
                  ? false
                  : { opacity: 0, scale: 0.75, y: 15 }
              }
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={
                reduceMotion
                  ? undefined
                  : { opacity: 0, scale: 0.9 }
              }
              transition={{
                type: "spring",
                stiffness: 260,
                damping: 20,
              }}
            >
              <div className={styles.fileIcon}>
                <FileText size={30} strokeWidth={1.8} />
              </div>

              <div className={styles.fileInfo}>
                <strong title={file.name}>{file.name}</strong>
                <span>
                  {(file.size / (1024 * 1024)).toFixed(2)} MB
                </span>
              </div>

              <motion.div
                className={styles.check}
                initial={reduceMotion ? false : { scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", delay: 0.12 }}
              >
                <CheckCircle2 size={24} />
              </motion.div>

              <button
                type="button"
                className={styles.removeButton}
                onClick={removeFile}
                aria-label="Remover arquivo"
                title="Remover arquivo"
              >
                <X size={20} />
              </button>
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {isSucking && file && (
            <motion.div
              key="suction"
              className={styles.suctionFile}
              initial={{
                x: origin.x,
                y: origin.y,
                scale: 1,
                opacity: 1,
                rotate: -8,
              }}
              animate={{
                x: 0,
                y: 0,
                scale: [1, 0.85, 0.1],
                opacity: [1, 1, 0],
                rotate: 0,
              }}
              transition={{
                duration: SUCTION_DURATION / 1000,
                ease: [0.55, 0, 0.85, 0.3],
                times: [0, 0.45, 1],
              }}
            >
              <FileText size={38} strokeWidth={1.6} />
              <span>PDF</span>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      <AnimatePresence mode="wait">
        {error && (
          <motion.div
            key={shake}
            className={styles.error}
            role="alert"
            initial={reduceMotion ? false : { opacity: 0, x: -8 }}
            animate={
              reduceMotion
                ? { opacity: 1 }
                : { opacity: 1, x: [0, -7, 7, -4, 4, 0] }
            }
            exit={{ opacity: 0 }}
            transition={{ duration: 0.4 }}
          >
            <AlertCircle size={18} />
            {error}
          </motion.div>
        )}
      </AnimatePresence>

      {phase === "selected" && file && (
        <motion.div
          className={styles.actions}
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <button
            type="button"
            className={styles.analyzeButton}
            disabled
            title="A integração com o backend será implementada na próxima etapa"
          >
            Analisar meu extrato
          </button>

          <button
            type="button"
            className={styles.changeButton}
            onClick={open}
          >
            Escolher outro arquivo
          </button>
        </motion.div>
      )}
    </div>
  );
}

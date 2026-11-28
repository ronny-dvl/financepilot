import { useCallback, useEffect, useRef, useState } from "react";

import { useDropzone, type FileRejection } from "react-dropzone";
import { useNavigate } from "react-router-dom";

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

import { analyzeStatement } from "../../services/statementApi";
import { useStatement } from "../../contexts/StatementContext";
import { useBackendReadiness } from "../../contexts/BackendReadinessContext";
import AnalysisTransition, { type AnalysisStep } from "../AnalysisTransition/AnalysisTransition";

import styles from "./PdfUpload.module.css";

const MAX_SIZE = 10 * 1024 * 1024;

const SUCTION_DURATION = 650;
// Allow Render's free instance to wake up before processing the PDF.
const ANALYSIS_TIMEOUT_MS = 180_000;

type AnimationPhase = "idle" | "sucking" | "selected";

export default function PdfUpload() {
  const navigate = useNavigate();
  const { setAnalysis } = useStatement();
  const { status: backendStatus, waitUntilReady } = useBackendReadiness();

  const [file, setFile] = useState<File | null>(null);

  const [error, setError] = useState("");

  const [shake, setShake] = useState(0);

  const [phase, setPhase] = useState<AnimationPhase>("idle");

  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const reduceMotion = useReducedMotion();
  const [analysisStep, setAnalysisStep] = useState<AnalysisStep | null>(null);
  const [progress, setProgress] = useState(0);
  const progressRef = useRef(0);
  const completedRef = useRef(false);
  const [waitingForBackend, setWaitingForBackend] = useState(false);
  const requestRef = useRef<AbortController | null>(null);
  const mountedRef = useRef(false);
  const navigatedRef = useRef(false);

  const goToDashboard = useCallback(() => {
    if (navigatedRef.current) return;
    navigatedRef.current = true;
    navigate("/dashboard");
  }, [navigate]);

  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; requestRef.current?.abort(); };
  }, []);

  useEffect(() => {
    if (analysisStep !== 3) return;
    const timer = setTimeout(goToDashboard, reduceMotion ? 0 : 650);
    return () => clearTimeout(timer);
  }, [analysisStep, reduceMotion, goToDashboard]);

  useEffect(() => {
    if (!isAnalyzing) return;
    // This is a visual estimate. The successful API response is the only gate to 100%.
    const timer = setInterval(() => {
      if (!requestRef.current || requestRef.current.signal.aborted) return;
      const current = progressRef.current;
      const target = waitingForBackend ? 24 : current < 25 ? 25 : current < 60 ? 60
        : current < 99 ? 99 : completedRef.current ? 100 : 99;
      const increment = waitingForBackend ? 0.12 : completedRef.current ? (reduceMotion ? 100 : 3)
        : current < 25 ? 0.35 : current < 60 ? 0.25 : Math.max(0.015, (99 - current) * 0.007);
      const next = Math.min(target, current + increment);
      if (next <= current) return;
      progressRef.current = next;
      setProgress(next);
      setAnalysisStep(next === 100 ? 3 : next >= 60 ? 2 : next >= 25 ? 1 : 0);
    }, 50);
    return () => clearInterval(timer);
  }, [isAnalyzing, waitingForBackend, reduceMotion]);

  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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

      if (isAnalyzing) return;

      clearAnimation();

      setFile(selected);

      setError("");
      setAnalysisStep(null);
      progressRef.current = 0;
      completedRef.current = false;
      setProgress(0);

      if (reduceMotion) {

        setPhase("selected");
        setAnalysisStep(0);

        return;

      }

      setPhase("sucking");

      timerRef.current = setTimeout(() => {

        setPhase("selected");
        setAnalysisStep(0);

        timerRef.current = null;

      }, SUCTION_DURATION);

    },

    [clearAnimation, reduceMotion, isAnalyzing]

  );

  const onDrop = useCallback(

    (accepted: File[]) => {

      if (!accepted.length || isAnalyzing) return;

      selectFile(accepted[0]);

    },

    [selectFile, isAnalyzing]

  );

  const onDropRejected = useCallback(

    (rejections: FileRejection[]) => {

      if (isAnalyzing) return;

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

    [isAnalyzing]

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

    disabled: isAnalyzing,

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

    if (isAnalyzing) return;

    clearAnimation();

    setFile(null);

    setError("");

    setPhase("idle");

  };

  const handleAnalyze = useCallback(async () => {
    if (!file || isAnalyzing || requestRef.current) return;

    const controller = new AbortController();
    requestRef.current = controller;
    let timeout: ReturnType<typeof setTimeout> | undefined;
    let succeeded = false;
    navigatedRef.current = false;
    progressRef.current = 0;
    completedRef.current = false;
    setProgress(0);
    setAnalysisStep(0);
    setWaitingForBackend(true);
    setIsAnalyzing(true);
    setError("");

    try {
      await waitUntilReady(controller.signal);
      if (!mountedRef.current || requestRef.current !== controller || controller.signal.aborted) return;
      setWaitingForBackend(false);
      timeout = setTimeout(() => controller.abort(), ANALYSIS_TIMEOUT_MS);
      const analysis = await analyzeStatement(file, controller.signal);
      if (!mountedRef.current || requestRef.current !== controller || controller.signal.aborted) return;

      setAnalysis(analysis);
      succeeded = true;
      completedRef.current = true;
      if (reduceMotion) {
        progressRef.current = 100;
        setProgress(100);
        setAnalysisStep(3);
      }
    } catch (err) {
      if (!mountedRef.current || requestRef.current !== controller) return;
      setAnalysisStep(null);
      setError(controller.signal.aborted
        ? "A análise demorou mais que o esperado. Tente novamente."
        : err instanceof Error ? err.message : "Ocorreu um erro inesperado ao analisar o extrato.");
      setShake(value => value + 1);
    } finally {
      clearTimeout(timeout);
      if (mountedRef.current && requestRef.current === controller) {
        setWaitingForBackend(false);
        // Keep the attempt locked until the success animation finishes or the user cancels.
        if (!succeeded) {
          requestRef.current = null;
          setIsAnalyzing(false);
        }
      }
    }
  }, [file, isAnalyzing, setAnalysis, waitUntilReady, reduceMotion]);

  useEffect(() => {
    if (analysisStep !== 0 || !file || isAnalyzing || requestRef.current) return;
    const timer = setTimeout(() => { void handleAnalyze(); }, reduceMotion ? 0 : 450);
    return () => clearTimeout(timer);
  }, [analysisStep, file, handleAnalyze, reduceMotion, isAnalyzing]);

  const isSucking = phase === "sucking";

  const returnToUpload = () => {
    requestRef.current?.abort();
    requestRef.current = null;
    completedRef.current = false;
    setWaitingForBackend(false);
    setIsAnalyzing(false);
    setAnalysisStep(null);
  };

  return (

    <div className={styles.container}>
      {analysisStep !== null && (
        <AnalysisTransition step={analysisStep} progress={progress}
          waitingForServer={waitingForBackend && backendStatus === "waiting"} onCancel={returnToUpload} />
      )}

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

                disabled={isAnalyzing}

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

                disabled={isAnalyzing}

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

            onClick={handleAnalyze}

            disabled={isAnalyzing}

            aria-busy={isAnalyzing}

          >

            {isAnalyzing

              ? "Analisando extrato..."

              : "Analisar meu extrato"}

          </button>

          <button

            type="button"

            className={styles.changeButton}

            onClick={open}

            disabled={isAnalyzing}

          >

            Escolher outro arquivo

          </button>

        </motion.div>

      )}

    </div>

  );

}

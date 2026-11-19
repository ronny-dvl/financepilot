import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { checkBackendHealth } from "../services/statementApi";

type Status = "idle" | "checking" | "waiting" | "ready" | "error";
type Timer = ReturnType<typeof setTimeout>;
type Check = { controller?: AbortController; slow?: Timer; deadline?: Timer; requestTimeout?: Timer; retry?: Timer };
type Readiness = { status: Status; ensureReady: (force?: boolean) => void; cancel: () => void };
const BackendReadinessContext = createContext<Readiness | undefined>(undefined);

function stop(check: Check) {
  check.controller?.abort();
  clearTimeout(check.slow);
  clearTimeout(check.deadline);
  clearTimeout(check.requestTimeout);
  clearTimeout(check.retry);
}

export function BackendReadinessProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>("idle");
  const active = useRef<Check | null>(null);
  const lastHealthyAt = useRef(0);

  const cancel = useCallback(() => {
    const check = active.current;
    if (!check) return;
    active.current = null;
    stop(check);
    setStatus("idle");
  }, []);

  const ensureReady = useCallback((force = false) => {
    // Share the in-flight check across entry, guest form, and upload route.
    if (active.current) return;
    if (!force && Date.now() - lastHealthyAt.current < 30_000) {
      setStatus("ready");
      return;
    }
    const check: Check = {};
    active.current = check;
    setStatus("checking");

    const finish = (result: "ready" | "error") => {
      if (active.current !== check) return;
      active.current = null;
      stop(check);
      if (result === "ready") lastHealthyAt.current = Date.now();
      setStatus(result);
    };

    check.slow = setTimeout(() => {
      if (active.current === check) setStatus("waiting");
    }, 800);
    check.deadline = setTimeout(() => finish("error"), 180_000);

    const attempt = async () => {
      if (active.current !== check) return;
      const controller = new AbortController();
      check.controller = controller;
      const timeout = setTimeout(() => controller.abort(), 15_000);
      check.requestTimeout = timeout;
      try {
        await checkBackendHealth(controller.signal);
        finish("ready");
      } catch {
        if (active.current === check) {
          check.retry = setTimeout(() => { void attempt(); }, 3_000);
        }
      } finally {
        clearTimeout(timeout);
      }
    };
    void attempt();
  }, []);

  useEffect(() => () => {
    const check = active.current;
    active.current = null;
    if (check) stop(check);
  }, []);

  return <BackendReadinessContext.Provider value={{ status, ensureReady, cancel }}>{children}</BackendReadinessContext.Provider>;
}

export function useBackendReadiness() {
  const readiness = useContext(BackendReadinessContext);
  if (!readiness) throw new Error("useBackendReadiness deve ser usado dentro de BackendReadinessProvider.");
  return readiness;
}

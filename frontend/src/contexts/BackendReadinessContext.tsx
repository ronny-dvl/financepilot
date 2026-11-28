import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { checkBackendHealth } from "../services/statementApi";

type Status = "idle" | "checking" | "waiting" | "ready" | "error";
type Timer = ReturnType<typeof setTimeout>;
type Check = { controller?: AbortController; slow?: Timer; deadline?: Timer; requestTimeout?: Timer; retry?: Timer };
type Readiness = { status: Status; ensureReady: (force?: boolean) => void; waitUntilReady: (signal: AbortSignal) => Promise<void>; cancel: () => void };
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
  const listeners = useRef(new Set<(next: Status) => void>());
  const updateStatus = useCallback((next: Status) => {
    setStatus(next);
    listeners.current.forEach(listener => listener(next));
  }, []);

  const cancel = useCallback(() => {
    const check = active.current;
    if (!check) return;
    active.current = null;
    stop(check);
    updateStatus("idle");
  }, [updateStatus]);

  const ensureReady = useCallback((force = false) => {
    // Share the in-flight check across entry, guest form, and upload route.
    if (active.current) return;
    if (!force && Date.now() - lastHealthyAt.current < 30_000) {
      updateStatus("ready");
      return;
    }
    const check: Check = {};
    active.current = check;
    updateStatus("checking");

    const finish = (result: "ready" | "error") => {
      if (active.current !== check) return;
      active.current = null;
      stop(check);
      if (result === "ready") lastHealthyAt.current = Date.now();
      updateStatus(result);
    };

    check.slow = setTimeout(() => {
      if (active.current === check) updateStatus("waiting");
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
  }, [updateStatus]);

  const waitUntilReady = useCallback((signal: AbortSignal): Promise<void> => new Promise((resolve, reject) => {
    const cleanup = () => {
      listeners.current.delete(onStatus);
      signal.removeEventListener("abort", onAbort);
    };
    const onAbort = () => { cleanup(); reject(new DOMException("Análise cancelada", "AbortError")); };
    const onStatus = (next: Status) => {
      if (next === "ready") { cleanup(); resolve(); }
      else if (next === "error") { cleanup(); reject(new Error("O servidor ainda não respondeu. Tente novamente.")); }
      else if (next === "idle") onAbort();
    };
    if (signal.aborted) { onAbort(); return; }
    listeners.current.add(onStatus);
    signal.addEventListener("abort", onAbort, { once: true });
    ensureReady();
  }), [ensureReady]);

  useEffect(() => () => {
    const check = active.current;
    active.current = null;
    if (check) stop(check);
    listeners.current.forEach(listener => listener("idle"));
    listeners.current.clear();
  }, []);

  return <BackendReadinessContext.Provider value={{ status, ensureReady, waitUntilReady, cancel }}>{children}</BackendReadinessContext.Provider>;
}

export function useBackendReadiness() {
  const readiness = useContext(BackendReadinessContext);
  if (!readiness) throw new Error("useBackendReadiness deve ser usado dentro de BackendReadinessProvider.");
  return readiness;
}

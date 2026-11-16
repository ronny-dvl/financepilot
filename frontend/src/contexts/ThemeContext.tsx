import { createContext, useContext, useLayoutEffect, useState, type ReactNode } from "react";

export type ThemePreference = "light" | "dark" | "system";
const STORAGE_KEY = "financepilot.theme";
const isPreference = (value: string | null): value is ThemePreference =>
  value === "light" || value === "dark" || value === "system";

const ThemeContext = createContext<{
  preference: ThemePreference;
  setPreference: (value: ThemePreference) => void;
} | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [preference, setPreferenceState] = useState<ThemePreference>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return isPreference(saved) ? saved : "system";
    } catch { return "system"; }
  });

  useLayoutEffect(() => {
    const system = window.matchMedia("(prefers-color-scheme: dark)");
    const apply = () => {
      const resolved = preference === "system" ? (system.matches ? "dark" : "light") : preference;
      document.documentElement.dataset.theme = resolved;
      document.documentElement.style.colorScheme = resolved;
    };
    apply();
    system.addEventListener("change", apply);
    const sync = (event: StorageEvent) => {
      if (event.key === STORAGE_KEY || event.key === null) {
        setPreferenceState(isPreference(event.newValue) ? event.newValue : "system");
      }
    };
    window.addEventListener("storage", sync);
    return () => {
      system.removeEventListener("change", apply);
      window.removeEventListener("storage", sync);
    };
  }, [preference]);

  const setPreference = (value: ThemePreference) => {
    setPreferenceState(value);
    try { localStorage.setItem(STORAGE_KEY, value); } catch { /* Keep the session preference. */ }
  };

  return <ThemeContext.Provider value={{ preference, setPreference }}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const theme = useContext(ThemeContext);
  if (!theme) throw new Error("useTheme deve ser usado dentro de ThemeProvider.");
  return theme;
}

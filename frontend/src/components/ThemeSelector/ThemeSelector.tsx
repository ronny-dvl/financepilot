import { Sun, Moon, Monitor } from "lucide-react";
import { useTheme, type ThemePreference } from "../../contexts/ThemeContext";
import styles from "./ThemeSelector.module.css";

export default function ThemeSelector() {
  const { preference, setPreference } = useTheme();
  const Icon = preference === "system" ? Monitor : preference === "dark" ? Moon : Sun;
  return (
    <label className={styles.control}>
      <Icon size={16} aria-hidden="true" />
      <select aria-label="Tema de aparência" value={preference}
        onChange={event => setPreference(event.target.value as ThemePreference)}>
        <option value="light">Claro</option>
        <option value="dark">Escuro</option>
        <option value="system">Sistema</option>
      </select>
    </label>
  );
}

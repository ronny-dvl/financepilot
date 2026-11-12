
import type { TargetAndTransition, Transition } from "framer-motion";

/**
 * Configuração de animação dos 6 personagens da Seção 3.
 *
 * 0.00s - 0.36s: Repouso no centro
 * 0.36s - 0.96s: Expansão orbital
 * 0.96s - 8.96s: Rotação anti-horária de 360°
 * 8.96s - 9.56s: Recolhimento ao centro
 * 9.56s - 10.40s: Repouso e reinício do loop
 */

export interface CharacterConfig {
  id: string;
  face: string;
  angleDeg: number;
  zIndex: number;
  isHero?: boolean;
}

// Raios da órbita em unidades cqw
const RX_CQW = 38;
const RY_CQW = 33;

// Configuração de velocidade
// Altere apenas ROTATION_DURATION para ajustar o giro.
const ROTATION_DURATION = 5;

// Durações das outras etapas
const INITIAL_REST = 0.36;
const EXPANSION_DURATION = 0.6;
const COLLAPSE_DURATION = 0.6;
const FINAL_REST = 0.84;

const TOTAL_DURATION =
  INITIAL_REST +
  EXPANSION_DURATION +
  ROTATION_DURATION +
  COLLAPSE_DURATION +
  FINAL_REST;

/**
 * Ordem orbital dos personagens.
 */
export const CHARACTERS_LAYOUT: CharacterConfig[] = [
  { id: "char-2", face: "==", angleDeg: 0, zIndex: 3 },
  { id: "char-1", face: "><", angleDeg: 60, zIndex: 10, isHero: true },
  { id: "char-6", face: "o <", angleDeg: 120, zIndex: 4 },
  { id: "char-5", face: "||", angleDeg: 180, zIndex: 5 },
  { id: "char-4", face: "^ ^", angleDeg: 240, zIndex: 6 },
  { id: "char-3", face: "++", angleDeg: 300, zIndex: 7 },
];

/**
 * Keyframes normalizados.
 *
 * A rotação possui 6 intervalos de 60 graus.
 * Os demais movimentos mantêm a duração original.
 */
const rotationStart = INITIAL_REST + EXPANSION_DURATION;
const rotationEnd = rotationStart + ROTATION_DURATION;
const collapseEnd = rotationEnd + COLLAPSE_DURATION;
const finalRestStart = collapseEnd + 0.48;

const KEYFRAME_TIMES = [
  0,
  INITIAL_REST,
  rotationStart,
  ...Array.from(
    { length: 6 },
    (_, i) =>
      rotationStart +
      (ROTATION_DURATION * (i + 1)) / 6
  ),
  collapseEnd,
  finalRestStart,
  TOTAL_DURATION,
].map((time) => time / TOTAL_DURATION);

/**
 * Curvas de aceleração.
 */
const KEYFRAME_EASES = [
  "linear",
  "easeOut",
  "linear",
  "linear",
  "linear",
  "linear",
  "linear",
  "linear",
  "easeInOut",
  "linear",
  "linear",
];

/**
 * Calcula a posição orbital de cada personagem.
 */
function getOrbitPoint(deg: number) {
  const rad = (deg * Math.PI) / 180;

  return {
    x: `${(RX_CQW * Math.cos(rad)).toFixed(2)}cqw`,
    y: `${(RY_CQW * Math.sin(rad)).toFixed(2)}cqw`,
  };
}

/**
 * Gera a animação individual de cada personagem.
 */
export function getCharacterMotion(
  char: CharacterConfig
): {
  animate: TargetAndTransition;
  transition: Transition;
} {
  const steps = [0, 60, 120, 180, 240, 300, 360];

  const orbitPoints = steps.map((step) =>
    getOrbitPoint(char.angleDeg - step)
  );

  const x = [
    "0cqw",
    "0cqw",
    ...orbitPoints.map((point) => point.x),
    "0cqw",
    "0cqw",
    "0cqw",
  ];

  const y = [
    "0cqw",
    "0cqw",
    ...orbitPoints.map((point) => point.y),
    "0cqw",
    "0cqw",
    "0cqw",
  ];

  const opacity = char.isHero
    ? [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    : [0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0];

  const scale = char.isHero
    ? [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]
    : [
        0.85, 0.85,
        1, 1, 1, 1, 1, 1, 1,
        0.85, 0.85, 0.85,
      ];

  return {
    animate: {
      x,
      y,
      opacity,
      scale,
    },

    transition: {
      duration: TOTAL_DURATION,
      repeat: Infinity,
      times: KEYFRAME_TIMES,
      ease: KEYFRAME_EASES,
    },
  };
}

/**
 * Entrada suave dos cards da coluna direita.
 */
export const cardsContainerVariants = {
  hidden: { opacity: 0 },

  visible: {
    opacity: 1,

    transition: {
      staggerChildren: 0.15,
      delayChildren: 0.2,
    },
  },
};

export const cardItemVariants = {
  hidden: {
    opacity: 0,
    y: 20,
  },

  visible: {
    opacity: 1,
    y: 0,

    transition: {
      duration: 0.6,
      ease: "easeOut",
    },
  },
};

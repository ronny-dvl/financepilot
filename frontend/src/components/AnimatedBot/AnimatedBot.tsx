
import { useEffect, useRef } from "react";
import botBody from "../../assets/characters/Bot_1_sem_olhos.svg";
import "./AnimatedBot.css";

type Props = {
  size?: number;
  heroSelector?: string;
  className?: string;
};

export default function AnimatedBot({
  size = 180,
  heroSelector = ".hero",
  className = "",
}: Props) {
  const root = useRef<HTMLDivElement>(null);
  const eyes = useRef<(SVGGElement | null)[]>([]);

  useEffect(() => {
    const element = root.current;
    if (!element) return;

    const hero = element.closest(heroSelector);
    if (!hero) return;

    const reduced = window.matchMedia(
      "(prefers-reduced-motion: reduce)"
    ).matches;

    const state = {
      x: 0,
      y: 0,
      vx: 0,
      vy: 0,
      targetX: 0,
      targetY: 0,
    };

    let frame = 0;
    let visible = true;
    let previous = 0;

    const clamp = (value: number, min: number, max: number) =>
      Math.min(max, Math.max(min, value));

    const move = (event: Event) => {
      if (reduced) return;

      const pointer = event as PointerEvent;
      const rect = element.getBoundingClientRect();

      const dx = pointer.clientX - (rect.left + rect.width / 2);
      const dy = pointer.clientY - (rect.top + rect.height / 2);

      const distance = Math.hypot(dx, dy) || 1;
      const strength = Math.min(
        0.75,
        Math.tanh(distance / 180) * 0.9
      );

      state.targetX = (dx / distance) * strength;
      state.targetY = (dy / distance) * strength;
    };

    const reset = () => {
      state.targetX = 0;
      state.targetY = 0;
    };

    const observer = new IntersectionObserver(([entry]) => {
      visible = entry.isIntersecting;
    });

    observer.observe(element);

    const tick = (now: number) => {
      const dt = previous
        ? Math.min(2.5, (now - previous) / 16.67)
        : 1;

      previous = now;

      if (visible && !document.hidden) {
        if (reduced) {
          state.x = 0;
          state.y = 0;
        } else {
          const stiffness = 0.06;
          const damping = 0.22;

          state.vx +=
            ((state.targetX - state.x) * stiffness -
              state.vx * damping) * dt;

          state.vy +=
            ((state.targetY - state.y) * stiffness -
              state.vy * damping) * dt;

          state.x += state.vx * dt;
          state.y += state.vy * dt;
        }

        const yaw = Math.asin(clamp(state.x, -0.85, 0.85));
        const pitch = -Math.asin(clamp(state.y, -0.85, 0.85));

        const cy = Math.cos(yaw);
        const sy = Math.sin(yaw);
        const cp = Math.cos(pitch);
        const sp = Math.sin(pitch);

        // Centro e distância dos olhos no SVG original
        const centerX = 135; // Move os dois olhos para a esquerda
        const centerY = 190; // Mantém a altura
        const radius = 80;  // Mantém a amplitude do movimento
        const eyeGap = 0.35; // Aproxima os olhos

        [-eyeGap, eyeGap].forEach((x0, index) => {
          const eye = eyes.current[index];
          if (!eye) return;

          const z0 = Math.sqrt(1 - x0 * x0);

          const x1 = x0 * cy + z0 * sy;
          const z1 = -x0 * sy + z0 * cy;

          const y2 = -z1 * sp;
          const z2 = z1 * cp;

          const depth = clamp(z2, 0, 1);

          // Perspectiva: o olho lateral fica mais estreito
          const scaleX = 0.4 + 0.6 * depth;
          const scaleY = 0.75 + 0.25 * depth;

          const tilt = 25 * state.x * state.y;

          eye.setAttribute(
            "transform",
            `translate(${centerX + x1 * radius} ${
              centerY + y2 * radius
            }) rotate(${tilt}) scale(${scaleX} ${scaleY})`
          );

          eye.style.opacity = z2 < 0.05 ? "0" : "1";
        });
      }

      frame = requestAnimationFrame(tick);
    };

    hero.addEventListener("pointermove", move, {
      passive: true,
    });
    hero.addEventListener("pointerleave", reset);

    frame = requestAnimationFrame(tick);

    return () => {
      hero.removeEventListener("pointermove", move);
      hero.removeEventListener("pointerleave", reset);
      observer.disconnect();
      cancelAnimationFrame(frame);
    };
  }, [heroSelector]);

  return (
    <div
      ref={root}
      className={`animated-bot ${className}`}
      style={{ width: `min(100%, ${size}px)` }}
      role="img"
      aria-label="Mascote do FinancePilot com olhos animados"
    >
      <svg
        viewBox="0 0 318 355"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <image
          href={botBody}
          width="318"
          height="355"
        />

        {[-1, 1].map((value, index) => (
          <g
            key={value}
            ref={(node) => {
              eyes.current[index] = node;
            }}
          >
            <rect
  className="animated-bot__eye"
  x="-16"
  y="-30"
  width="32"
  height="60"
  rx="16"
  fill="#060606"
/>
          </g>
        ))}
      </svg>
    </div>
  );
}

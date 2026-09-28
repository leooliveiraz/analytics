import { useEffect, useRef } from "react";
import type { HeatmapPoint } from "../api/types";

interface HeatmapCanvasProps {
  points: HeatmapPoint[];
  maxWeight: number;
  type: string;
}

export function HeatmapCanvas({ points, maxWeight, type }: HeatmapCanvasProps) {
  const ref = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = ref.current;
    const parent = canvas?.parentElement;
    if (!canvas || !parent) {
      return;
    }
    const dpr = window.devicePixelRatio || 1;
    const width = parent.clientWidth;
    const height = parent.clientHeight;
    canvas.width = Math.max(1, Math.floor(width * dpr));
    canvas.height = Math.max(1, Math.floor(height * dpr));
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      return;
    }
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, width, height);
    const max = maxWeight || 1;

    if (type === "scroll") {
      points.forEach((point) => {
        const y = Math.min(height, (point.y / 100) * height);
        const alpha = Math.min(0.6, (point.weight / max) * 0.6);
        const gradient = ctx.createLinearGradient(0, y - 18, 0, y + 18);
        gradient.addColorStop(0, "rgba(110, 168, 254, 0)");
        gradient.addColorStop(0.5, `rgba(110, 168, 254, ${alpha})`);
        gradient.addColorStop(1, "rgba(110, 168, 254, 0)");
        ctx.fillStyle = gradient;
        ctx.fillRect(0, y - 18, width, 36);
      });
      return;
    }

    points.forEach((point) => {
      const x = (point.x / 100) * width;
      const y = (point.y / 100) * height;
      const ratio = point.weight / max;
      const radius = 8 + ratio * 22;
      const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius);
      gradient.addColorStop(0, `rgba(255, 80, 80, ${0.5 + ratio * 0.4})`);
      gradient.addColorStop(1, "rgba(255, 80, 80, 0)");
      ctx.fillStyle = gradient;
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fill();
    });
  }, [points, maxWeight, type]);

  return <canvas ref={ref} className="heatmap-canvas" />;
}

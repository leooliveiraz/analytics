import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { HeatmapResponse, Project } from "../api/types";
import { HeatmapCanvas } from "../components/HeatmapCanvas";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatNumber } from "../lib/format";

const TYPES = [
  { key: "click", label: "Cliques" },
  { key: "move", label: "Movimento" },
  { key: "scroll", label: "Scroll" },
];

const DEVICE_WIDTHS: Record<string, number> = { desktop: 1440, tablet: 834, mobile: 390 };

export function HeatmapPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();
  const [type, setType] = useState("click");
  const [path, setPath] = useState("/");
  const [device, setDevice] = useState("");
  const [pageUrl, setPageUrl] = useState("");
  const frameRef = useRef<HTMLDivElement | null>(null);
  const [frameWidth, setFrameWidth] = useState(0);

  useEffect(() => {
    const el = frameRef.current;
    if (!el) {
      return;
    }
    const update = () => setFrameWidth(el.clientWidth);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(el);
    window.addEventListener("resize", update);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", update);
    };
  }, []);

  const projectQuery = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => api<Project>(`/api/v1/projects/${projectId}`),
    enabled: Boolean(projectId),
  });

  const query = useQuery({
    queryKey: ["heatmap", projectId, from, to, type, path, device],
    queryFn: () => {
      const params = new URLSearchParams({ from, to, type, path });
      if (device) {
        params.set("device", device);
      }
      return api<HeatmapResponse>(`/api/v1/projects/${projectId}/heatmap?${params.toString()}`);
    },
    enabled: Boolean(projectId),
  });

  const project = projectQuery.data;
  const derivedUrl =
    project?.domain && path ? `https://${project.domain}${path.startsWith("/") ? path : `/${path}`}` : "";
  const previewUrl = pageUrl.trim() || derivedUrl;

  const data = query.data;
  const viewportWidth = data?.viewportWidth && data.viewportWidth > 0
    ? data.viewportWidth
    : DEVICE_WIDTHS[device] ?? 1440;
  const pageHeight = data?.pageHeight && data.pageHeight > 0 ? data.pageHeight : 1000;
  const scale = frameWidth > 0 ? Math.min(1, frameWidth / viewportWidth) : 1;
  const scaledWidth = Math.round(viewportWidth * scale);
  const scaledHeight = Math.round(pageHeight * scale);

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Mapa de calor</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <RangeTabs days={days} onChange={setDays} />
        <div className="tabs">
          {TYPES.map((item) => (
            <button
              key={item.key}
              type="button"
              className={type === item.key ? "active" : ""}
              onClick={() => setType(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      <div className="panel">
        <div className="row" style={{ flexWrap: "wrap", gap: 10 }}>
          <input placeholder="Página (ex: /)" value={path} onChange={(e) => setPath(e.target.value)} style={{ maxWidth: 200 }} />
          <select value={device} onChange={(e) => setDevice(e.target.value)} style={{ maxWidth: 170 }}>
            <option value="">Todos os dispositivos</option>
            <option value="desktop">Desktop</option>
            <option value="tablet">Tablet</option>
            <option value="mobile">Mobile</option>
          </select>
          <input
            placeholder="URL para pré-visualizar"
            value={pageUrl}
            onChange={(e) => setPageUrl(e.target.value)}
            style={{ flex: 1, minWidth: 240 }}
          />
          <span className="muted">{formatNumber(data?.points.length ?? 0)} pontos</span>
        </div>
      </div>

      <div className="panel">
        <div className="heatmap-frame" ref={frameRef}>
          <div className="heatmap-scaler" style={{ width: scaledWidth, height: scaledHeight }}>
            <div
              className="heatmap-content"
              style={{ width: viewportWidth, height: pageHeight, transform: `scale(${scale})` }}
            >
              {previewUrl ? (
                <iframe src={previewUrl} title="Pré-visualização" className="heatmap-iframe" loading="lazy" />
              ) : (
                <div className="heatmap-empty muted">
                  Informe a URL da página para sobrepor o mapa de calor.
                </div>
              )}
              <HeatmapCanvas points={data?.points ?? []} maxWeight={data?.maxWeight ?? 0} type={type} />
            </div>
          </div>
        </div>
        <p className="muted" style={{ fontSize: 12, marginTop: 8 }}>
          Renderizado na largura real gravada ({viewportWidth}px, escala {(scale * 100).toFixed(0)}%) e altura de{" "}
          {pageHeight}px. Selecione um dispositivo para evitar misturar layouts diferentes.
        </p>
      </div>
    </div>
  );
}

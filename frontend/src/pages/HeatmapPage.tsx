import { useState } from "react";
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

export function HeatmapPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();
  const [type, setType] = useState("click");
  const [path, setPath] = useState("/");
  const [device, setDevice] = useState("");
  const [pageUrl, setPageUrl] = useState("");

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
          <select value={device} onChange={(e) => setDevice(e.target.value)} style={{ maxWidth: 160 }}>
            <option value="">Todos os dispositivos</option>
            <option value="desktop">Desktop</option>
            <option value="mobile">Mobile</option>
            <option value="tablet">Tablet</option>
          </select>
          <input
            placeholder="URL para pré-visualizar"
            value={pageUrl}
            onChange={(e) => setPageUrl(e.target.value)}
            style={{ flex: 1, minWidth: 240 }}
          />
          <span className="muted">{formatNumber(query.data?.points.length ?? 0)} pontos</span>
        </div>
      </div>

      <div className="panel">
        <div className="heatmap-frame">
          {previewUrl ? (
            <iframe src={previewUrl} title="Pré-visualização" className="heatmap-iframe" loading="lazy" />
          ) : (
            <div className="heatmap-empty muted">
              Informe a URL da página para sobrepor o mapa de calor.
            </div>
          )}
          <HeatmapCanvas points={query.data?.points ?? []} maxWeight={query.data?.maxWeight ?? 0} type={type} />
        </div>
        <p className="muted" style={{ fontSize: 12, marginTop: 8 }}>
          O mapa é desenhado em porcentagem sobre a página. Se o site bloquear iframe, os pontos continuam visíveis
          sobre o painel.
        </p>
      </div>
    </div>
  );
}

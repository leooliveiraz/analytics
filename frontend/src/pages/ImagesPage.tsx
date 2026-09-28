import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { ImageStat } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatDuration, formatNumber } from "../lib/format";

export function ImagesPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();
  const [path, setPath] = useState("");

  const query = useQuery({
    queryKey: ["images", projectId, from, to, path],
    queryFn: () => {
      const suffix = path.trim() ? `&path=${encodeURIComponent(path.trim())}` : "";
      return api<ImageStat[]>(`/api/v1/projects/${projectId}/images?from=${from}&to=${to}&limit=100${suffix}`);
    },
    enabled: Boolean(projectId),
  });

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Imagens mais vistas</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <RangeTabs days={days} onChange={setDays} />
        <input
          placeholder="Filtrar por página"
          value={path}
          onChange={(e) => setPath(e.target.value)}
          style={{ maxWidth: 280 }}
        />
      </div>

      <div className="panel">
        {query.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th />
                <th>Imagem</th>
                <th className="right">Impressões</th>
                <th className="right">Sessões</th>
                <th className="right">Tempo médio</th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((row) => (
                <tr key={row.imageKey}>
                  <td style={{ width: 56 }}>
                    <img src={row.imageKey} alt="" className="thumb" loading="lazy" />
                  </td>
                  <td className="ellipsis" title={row.imageAlt ?? row.imageKey}>
                    {row.imageAlt || row.imageKey}
                  </td>
                  <td className="right">{formatNumber(row.impressions)}</td>
                  <td className="right">{formatNumber(row.sessions)}</td>
                  <td className="right">{formatDuration(row.avgDwellMs / 1000)}</td>
                </tr>
              ))}
              {(query.data ?? []).length === 0 && (
                <tr>
                  <td colSpan={5} className="muted">
                    Nenhuma imagem rastreada. Ative <code>data-images="true"</code> no snippet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { SectionStat } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatDuration, formatNumber } from "../lib/format";

export function SectionsPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();
  const [path, setPath] = useState("");

  const query = useQuery({
    queryKey: ["sections", projectId, from, to, path],
    queryFn: () => {
      const suffix = path.trim() ? `&path=${encodeURIComponent(path.trim())}` : "";
      return api<SectionStat[]>(`/api/v1/projects/${projectId}/sections?from=${from}&to=${to}&limit=100${suffix}`);
    },
    enabled: Boolean(projectId),
  });

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Tempo por seção</h1>
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
                <th>Seção</th>
                <th className="right">Visualizações</th>
                <th className="right">Visitantes</th>
                <th className="right">Tempo médio</th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((row) => (
                <tr key={row.sectionKey}>
                  <td className="ellipsis" title={row.sectionKey}>
                    {row.sectionKey}
                  </td>
                  <td className="right">{formatNumber(row.views)}</td>
                  <td className="right">{formatNumber(row.visitors)}</td>
                  <td className="right">{formatDuration(row.avgDwellMs / 1000)}</td>
                </tr>
              ))}
              {(query.data ?? []).length === 0 && (
                <tr>
                  <td colSpan={4} className="muted">
                    Nenhuma seção rastreada. Ative <code>data-sections="true"</code> e use{" "}
                    <code>data-analytics-section</code>.
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

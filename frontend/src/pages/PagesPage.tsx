import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { PageMetric } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatDuration, formatNumber } from "../lib/format";

export function PagesPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();

  const query = useQuery({
    queryKey: ["pages", projectId, from, to],
    queryFn: () => api<PageMetric[]>(`/api/v1/projects/${projectId}/pages?from=${from}&to=${to}&limit=100`),
    enabled: Boolean(projectId),
  });

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Páginas</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <RangeTabs days={days} onChange={setDays} />
        <span className="muted" style={{ fontSize: 13 }}>
          {from} → {to}
        </span>
      </div>

      <div className="panel">
        {query.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Página</th>
                <th className="right">Pageviews</th>
                <th className="right">Visitantes</th>
                <th className="right">Entradas</th>
                <th className="right">Tempo médio</th>
                <th className="right">Scroll médio</th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((row) => (
                <tr key={row.path}>
                  <td className="ellipsis" title={row.path}>
                    {row.path}
                  </td>
                  <td className="right">{formatNumber(row.pageviews)}</td>
                  <td className="right">{formatNumber(row.visitors)}</td>
                  <td className="right">{formatNumber(row.entries)}</td>
                  <td className="right">{formatDuration(row.avgTimeMs / 1000)}</td>
                  <td className="right">{row.avgScrollPct.toFixed(0)}%</td>
                </tr>
              ))}
              {(query.data ?? []).length === 0 && (
                <tr>
                  <td colSpan={6} className="muted">
                    Sem dados no período.
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

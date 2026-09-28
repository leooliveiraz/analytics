import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { GeoStat } from "../api/types";
import { GeoMap } from "../components/GeoMap";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatNumber } from "../lib/format";

export function GeoPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();

  const query = useQuery({
    queryKey: ["geo", projectId, from, to],
    queryFn: () => api<GeoStat[]>(`/api/v1/projects/${projectId}/geo?from=${from}&to=${to}`),
    enabled: Boolean(projectId),
  });

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Mapa de acessos</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <RangeTabs days={days} onChange={setDays} />
        <span className="muted" style={{ fontSize: 13 }}>
          {from} → {to}
        </span>
      </div>

      <div className="panel">
        {query.isLoading ? <p className="muted">Carregando...</p> : <GeoMap data={query.data ?? []} />}
      </div>

      <div className="panel">
        <table className="table">
          <thead>
            <tr>
              <th>País</th>
              <th className="right">Visitantes</th>
              <th className="right">Pageviews</th>
            </tr>
          </thead>
          <tbody>
            {(query.data ?? []).map((row) => (
              <tr key={row.country}>
                <td>{row.country}</td>
                <td className="right">{formatNumber(row.visitors)}</td>
                <td className="right">{formatNumber(row.pageviews)}</td>
              </tr>
            ))}
            {(query.data ?? []).length === 0 && (
              <tr>
                <td colSpan={3} className="muted">
                  Sem dados geográficos. Configure o GeoIP e reenvie eventos.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

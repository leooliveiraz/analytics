import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { ElementStat } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { RangeTabs } from "../components/RangeTabs";
import { useRange } from "../lib/useRange";
import { formatNumber } from "../lib/format";

export function ElementsPage() {
  const { projectId = "" } = useParams();
  const { days, setDays, from, to } = useRange();
  const [path, setPath] = useState("");

  const query = useQuery({
    queryKey: ["elements", projectId, from, to, path],
    queryFn: () => {
      const suffix = path.trim() ? `&path=${encodeURIComponent(path.trim())}` : "";
      return api<ElementStat[]>(`/api/v1/projects/${projectId}/elements?from=${from}&to=${to}&limit=100${suffix}`);
    },
    enabled: Boolean(projectId),
  });

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Elementos clicados</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <RangeTabs days={days} onChange={setDays} />
        <input
          placeholder="Filtrar por página (ex: /)"
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
                <th>Evento</th>
                <th>Seletor</th>
                <th>Texto</th>
                <th>Tag</th>
                <th className="right">Cliques</th>
                <th className="right">Visitantes</th>
                <th className="right">Sessões</th>
              </tr>
            </thead>
            <tbody>
              {(query.data ?? []).map((row) => (
                <tr key={`${row.eventName}:${row.selector}`}>
                  <td>
                    <span className="badge">{row.eventName}</span>
                  </td>
                  <td className="ellipsis" title={row.selector}>
                    {row.selector}
                  </td>
                  <td className="ellipsis" title={row.text ?? ""}>
                    {row.text ?? "-"}
                  </td>
                  <td>{row.tag ?? "-"}</td>
                  <td className="right">{formatNumber(row.clicks)}</td>
                  <td className="right">{formatNumber(row.visitors)}</td>
                  <td className="right">{formatNumber(row.sessions)}</td>
                </tr>
              ))}
              {(query.data ?? []).length === 0 && (
                <tr>
                  <td colSpan={7} className="muted">
                    Nenhum clique rastreado. Adicione <code>data-analytics</code> aos elementos.
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

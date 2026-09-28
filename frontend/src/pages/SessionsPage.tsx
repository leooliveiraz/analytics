import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { PageResponse, SessionRow } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { formatDateTime, formatDuration, formatNumber } from "../lib/format";

const PAGE_SIZE = 50;

export function SessionsPage() {
  const { projectId = "" } = useParams();
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ["sessions", projectId, page],
    queryFn: () =>
      api<PageResponse<SessionRow>>(
        `/api/v1/projects/${projectId}/sessions?page=${page}&size=${PAGE_SIZE}`,
      ),
    enabled: Boolean(projectId),
    placeholderData: keepPreviousData,
  });

  const data = query.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Sessões</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="panel">
        {query.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Início</th>
                <th>Visitante</th>
                <th>Entrada</th>
                <th>Saída</th>
                <th className="right">Páginas</th>
                <th className="right">Duração</th>
                <th>País</th>
                <th>Dispositivo</th>
              </tr>
            </thead>
            <tbody>
              {(data?.items ?? []).map((row) => (
                <tr key={row.id}>
                  <td>
                    <Link to={`/projects/${projectId}/sessions/${row.id}`}>{formatDateTime(row.startedAt)}</Link>
                  </td>
                  <td className="ellipsis" title={row.visitorId}>
                    {row.visitorId.slice(0, 10)}…
                  </td>
                  <td className="ellipsis" title={row.entryPath ?? ""}>
                    {row.entryPath ?? "-"}
                  </td>
                  <td className="ellipsis" title={row.exitPath ?? ""}>
                    {row.exitPath ?? "-"}
                  </td>
                  <td className="right">{formatNumber(row.pageviews)}</td>
                  <td className="right">{formatDuration(row.durationSeconds)}</td>
                  <td>{row.country ?? "-"}</td>
                  <td>{row.deviceType ?? "-"}</td>
                </tr>
              ))}
              {data && data.items.length === 0 && (
                <tr>
                  <td colSpan={8} className="muted">
                    Nenhuma sessão no período.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        <div className="row spread" style={{ marginTop: 14 }}>
          <button className="btn" type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Anterior
          </button>
          <span className="muted">
            Página {page + 1} de {totalPages}
          </span>
          <button
            className="btn"
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  );
}

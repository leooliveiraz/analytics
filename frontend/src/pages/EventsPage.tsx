import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { api } from "../api/client";
import type { EventRow, PageResponse } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { formatDateTime, formatNumber } from "../lib/format";

const PAGE_SIZE = 50;

export function EventsPage() {
  const { projectId = "" } = useParams();
  const [page, setPage] = useState(0);
  const [eventName, setEventName] = useState("");

  const query = useQuery({
    queryKey: ["events", projectId, page, eventName],
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) });
      if (eventName.trim()) {
        params.set("eventName", eventName.trim());
      }
      return api<PageResponse<EventRow>>(`/api/v1/projects/${projectId}/events?${params.toString()}`);
    },
    enabled: Boolean(projectId),
    placeholderData: keepPreviousData,
  });

  const data = query.data;
  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1;

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Eventos</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="panel">
        <div className="row" style={{ marginBottom: 14 }}>
          <input
            placeholder="Filtrar por nome do evento (ex: pageview)"
            value={eventName}
            onChange={(e) => {
              setEventName(e.target.value);
              setPage(0);
            }}
            style={{ maxWidth: 320 }}
          />
          <span className="muted">{data ? `${formatNumber(data.total)} eventos` : ""}</span>
        </div>

        {query.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Quando</th>
                <th>Evento</th>
                <th>Página</th>
                <th>Referrer</th>
                <th>País</th>
                <th>Dispositivo</th>
                <th>Navegador</th>
              </tr>
            </thead>
            <tbody>
              {(data?.items ?? []).map((row) => (
                <tr key={row.id}>
                  <td>{formatDateTime(row.occurredAt)}</td>
                  <td>
                    <span className="badge">{row.eventName}</span>
                  </td>
                  <td className="ellipsis" title={row.path ?? ""}>
                    {row.path ?? "-"}
                  </td>
                  <td>{row.referrerDomain ?? "direct"}</td>
                  <td>{row.country ?? "-"}</td>
                  <td>{row.deviceType ?? "-"}</td>
                  <td>{row.browser ?? "-"}</td>
                </tr>
              ))}
              {data && data.items.length === 0 && (
                <tr>
                  <td colSpan={7} className="muted">
                    Nenhum evento no período.
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

import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../api/client";
import type { SessionDetail } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { StatCard } from "../components/StatCard";
import { formatDateTime, formatDuration, formatNumber } from "../lib/format";

export function SessionDetailPage() {
  const { projectId = "", sessionId = "" } = useParams();

  const query = useQuery({
    queryKey: ["session", projectId, sessionId],
    queryFn: () => api<SessionDetail>(`/api/v1/projects/${projectId}/sessions/${sessionId}`),
    enabled: Boolean(projectId && sessionId),
  });

  const session = query.data?.session;

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Detalhe da sessão</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <p>
        <Link to={`/projects/${projectId}/sessions`}>← Voltar para sessões</Link>
      </p>

      {query.isLoading ? (
        <p className="muted">Carregando...</p>
      ) : !session ? (
        <p className="muted">Sessão não encontrada.</p>
      ) : (
        <>
          <div className="grid cards-4">
            <StatCard label="Visitante" value={`${session.visitorId.slice(0, 10)}…`} />
            <StatCard label="Páginas" value={formatNumber(session.pageviews)} />
            <StatCard label="Duração" value={formatDuration(session.durationSeconds)} />
            <StatCard label="País" value={session.country ?? "-"} />
            <StatCard label="Dispositivo" value={session.deviceType ?? "-"} />
            <StatCard label="Navegador" value={session.browser ?? "-"} />
            <StatCard label="Sistema" value={session.os ?? "-"} />
            <StatCard label="Referrer" value={session.referrerDomain ?? "direct"} />
          </div>

          <div className="panel">
            <h2>Linha do tempo</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>Quando</th>
                  <th>Evento</th>
                  <th>Página</th>
                </tr>
              </thead>
              <tbody>
                {(query.data?.events ?? []).map((event) => (
                  <tr key={event.id}>
                    <td>{formatDateTime(event.occurredAt)}</td>
                    <td>
                      <span className="badge">{event.eventName}</span>
                    </td>
                    <td className="ellipsis" title={event.path ?? ""}>
                      {event.path ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}

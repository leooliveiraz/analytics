import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { api } from "../api/client";
import type {
  BreakdownResponse,
  Overview,
  Project,
  RealtimeResponse,
  StatsResponse,
} from "../api/types";
import { BreakdownTable } from "../components/BreakdownTable";
import { ProjectNav } from "../components/ProjectNav";
import { StatCard } from "../components/StatCard";
import { TimeseriesChart } from "../components/TimeseriesChart";
import { daysAgo, formatDuration, formatNumber, formatPercent, today } from "../lib/format";

const DIMENSIONS: { key: string; label: string }[] = [
  { key: "path", label: "Páginas" },
  { key: "referrer", label: "Referrers" },
  { key: "country", label: "Países" },
  { key: "device", label: "Dispositivos" },
  { key: "browser", label: "Navegadores" },
  { key: "os", label: "Sistemas" },
  { key: "utm_source", label: "UTM source" },
  { key: "event", label: "Eventos" },
];

const RANGES = [
  { days: 7, label: "7 dias" },
  { days: 30, label: "30 dias" },
  { days: 90, label: "90 dias" },
];

export function DashboardPage() {
  const { projectId = "" } = useParams();
  const [days, setDays] = useState(7);
  const [dimension, setDimension] = useState("path");
  const from = daysAgo(days - 1);
  const to = today();

  const projectQuery = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => api<Project>(`/api/v1/projects/${projectId}`),
    enabled: Boolean(projectId),
  });

  const overviewQuery = useQuery({
    queryKey: ["overview", projectId, from, to],
    queryFn: () => api<Overview>(`/api/v1/projects/${projectId}/overview?from=${from}&to=${to}`),
    enabled: Boolean(projectId),
  });

  const statsQuery = useQuery({
    queryKey: ["stats", projectId, from, to, "day"],
    queryFn: () => api<StatsResponse>(`/api/v1/projects/${projectId}/stats?from=${from}&to=${to}&interval=day`),
    enabled: Boolean(projectId),
  });

  const breakdownQuery = useQuery({
    queryKey: ["breakdown", projectId, from, to, dimension],
    queryFn: () =>
      api<BreakdownResponse>(
        `/api/v1/projects/${projectId}/breakdown?from=${from}&to=${to}&dimension=${dimension}&limit=12`,
      ),
    enabled: Boolean(projectId),
  });

  const realtimeQuery = useQuery({
    queryKey: ["realtime", projectId],
    queryFn: () => api<RealtimeResponse>(`/api/v1/projects/${projectId}/realtime?minutes=30`),
    enabled: Boolean(projectId),
    refetchInterval: 15_000,
  });

  const overview = overviewQuery.data;

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>{projectQuery.data?.name ?? "Projeto"}</h1>
        <ProjectNav projectId={projectId} />
      </div>

      <div className="row spread">
        <div className="tabs">
          {RANGES.map((range) => (
            <button
              key={range.days}
              type="button"
              className={days === range.days ? "active" : ""}
              onClick={() => setDays(range.days)}
            >
              {range.label}
            </button>
          ))}
        </div>
        <span className="muted" style={{ fontSize: 13 }}>
          {from} → {to}
        </span>
      </div>

      <div className="grid cards-4">
        <StatCard label="Visitantes" value={formatNumber(overview?.visitors ?? 0)} />
        <StatCard label="Pageviews" value={formatNumber(overview?.pageviews ?? 0)} />
        <StatCard label="Sessões" value={formatNumber(overview?.sessions ?? 0)} />
        <StatCard label="Taxa de rejeição" value={formatPercent(overview?.bounceRate ?? 0)} />
        <StatCard label="Duração média" value={formatDuration(overview?.avgDurationSeconds ?? 0)} />
        <StatCard label="Páginas/sessão" value={(overview?.pageviewsPerSession ?? 0).toFixed(2)} />
        <StatCard label="Ativos agora" value={formatNumber(realtimeQuery.data?.activeVisitors ?? 0)} />
        <StatCard label="Eventos" value={formatNumber(overview?.pageviews ?? 0)} />
      </div>

      <div className="panel">
        <h2>Evolução</h2>
        {statsQuery.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <TimeseriesChart points={statsQuery.data?.points ?? []} />
        )}
      </div>

      <div className="panel">
        <h2>Detalhamento</h2>
        <div className="tabs">
          {DIMENSIONS.map((item) => (
            <button
              key={item.key}
              type="button"
              className={dimension === item.key ? "active" : ""}
              onClick={() => setDimension(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
        {breakdownQuery.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <BreakdownTable items={breakdownQuery.data?.items ?? []} />
        )}
      </div>
    </div>
  );
}

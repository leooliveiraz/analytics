import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { api } from "../api/client";
import type {
  BreakdownResponse,
  DimensionTimeseriesResponse,
  FlowResponse,
  GeoStat,
  Overview,
  Project,
  RealtimeResponse,
  StatsResponse,
} from "../api/types";
import { BreakdownTable } from "../components/BreakdownTable";
import { ComparisonChart } from "../components/ComparisonChart";
import { DimensionTimeseriesChart } from "../components/DimensionTimeseriesChart";
import { DonutChart } from "../components/DonutChart";
import { GeoMap } from "../components/GeoMap";
import { HorizontalBarList } from "../components/HorizontalBarList";
import { ProjectNav } from "../components/ProjectNav";
import { StatCard } from "../components/StatCard";
import { UserFlowChart } from "../components/UserFlowChart";
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

const METRICS = [
  { key: "pageviews", label: "Pageviews" },
  { key: "visitors", label: "Visitantes" },
  { key: "sessions", label: "Sessões" },
] as const;

type Metric = (typeof METRICS)[number]["key"];

function delta(current: number, previous: number): number | null {
  if (!previous) {
    return null;
  }
  return (current - previous) / previous;
}

export function DashboardPage() {
  const { projectId = "" } = useParams();
  const [days, setDays] = useState(7);
  const [dimension, setDimension] = useState("path");
  const [metric, setMetric] = useState<Metric>("pageviews");
  const from = daysAgo(days - 1);
  const to = today();
  const previousFrom = daysAgo(days * 2 - 1);
  const previousTo = daysAgo(days);

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

  const previousOverviewQuery = useQuery({
    queryKey: ["overview", projectId, previousFrom, previousTo],
    queryFn: () =>
      api<Overview>(`/api/v1/projects/${projectId}/overview?from=${previousFrom}&to=${previousTo}`),
    enabled: Boolean(projectId),
  });

  const statsQuery = useQuery({
    queryKey: ["stats", projectId, from, to, "day"],
    queryFn: () =>
      api<StatsResponse>(`/api/v1/projects/${projectId}/stats?from=${from}&to=${to}&interval=day`),
    enabled: Boolean(projectId),
  });

  const previousStatsQuery = useQuery({
    queryKey: ["stats", projectId, previousFrom, previousTo, "day"],
    queryFn: () =>
      api<StatsResponse>(
        `/api/v1/projects/${projectId}/stats?from=${previousFrom}&to=${previousTo}&interval=day`,
      ),
    enabled: Boolean(projectId),
  });

  const pagesQuery = useQuery({
    queryKey: ["breakdown", projectId, from, to, "path", 8],
    queryFn: () =>
      api<BreakdownResponse>(
        `/api/v1/projects/${projectId}/breakdown?from=${from}&to=${to}&dimension=path&limit=8`,
      ),
    enabled: Boolean(projectId),
  });

  const referrersQuery = useQuery({
    queryKey: ["breakdown", projectId, from, to, "referrer", 8],
    queryFn: () =>
      api<BreakdownResponse>(
        `/api/v1/projects/${projectId}/breakdown?from=${from}&to=${to}&dimension=referrer&limit=8`,
      ),
    enabled: Boolean(projectId),
  });

  const devicesQuery = useQuery({
    queryKey: ["breakdown", projectId, from, to, "device", 6],
    queryFn: () =>
      api<BreakdownResponse>(
        `/api/v1/projects/${projectId}/breakdown?from=${from}&to=${to}&dimension=device&limit=6`,
      ),
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

  const geoQuery = useQuery({
    queryKey: ["geo", projectId, from, to],
    queryFn: () => api<GeoStat[]>(`/api/v1/projects/${projectId}/geo?from=${from}&to=${to}`),
    enabled: Boolean(projectId),
  });

  const browsersQuery = useQuery({
    queryKey: ["timeseries", projectId, from, to, "browser"],
    queryFn: () =>
      api<DimensionTimeseriesResponse>(
        `/api/v1/projects/${projectId}/timeseries?from=${from}&to=${to}&interval=day&dimension=browser&limit=5`,
      ),
    enabled: Boolean(projectId),
  });

  const flowQuery = useQuery({
    queryKey: ["flow", projectId, from, to],
    queryFn: () => api<FlowResponse>(`/api/v1/projects/${projectId}/flow?from=${from}&to=${to}&limit=15`),
    enabled: Boolean(projectId),
  });

  const realtimeQuery = useQuery({
    queryKey: ["realtime", projectId],
    queryFn: () => api<RealtimeResponse>(`/api/v1/projects/${projectId}/realtime?minutes=30`),
    enabled: Boolean(projectId),
    refetchInterval: 15_000,
  });

  const overview = overviewQuery.data;
  const previous = previousOverviewQuery.data;
  const geoTotal = (geoQuery.data ?? []).reduce((sum, item) => sum + item.visitors, 0);
  const metricLabel = METRICS.find((item) => item.key === metric)?.label ?? "Pageviews";

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
        <StatCard
          label="Pageviews"
          value={formatNumber(overview?.pageviews ?? 0)}
          sub={`${(overview?.pageviewsPerSession ?? 0).toFixed(1)} páginas/sessão`}
          delta={delta(overview?.pageviews ?? 0, previous?.pageviews ?? 0)}
        />
        <StatCard
          label="Sessões"
          value={formatNumber(overview?.sessions ?? 0)}
          sub={`duração média ${formatDuration(overview?.avgDurationSeconds ?? 0)}`}
          delta={delta(overview?.sessions ?? 0, previous?.sessions ?? 0)}
        />
        <StatCard
          label="Visitantes"
          value={formatNumber(overview?.visitors ?? 0)}
          sub={`${formatPercent(overview?.bounceRate ?? 0)} rejeição`}
          delta={delta(overview?.visitors ?? 0, previous?.visitors ?? 0)}
        />
        <StatCard
          label="Ativos agora"
          value={formatNumber(realtimeQuery.data?.activeVisitors ?? 0)}
          sub="últimos 30 min"
          accent
        />
      </div>

      <div className="panel">
        <div className="panel-head">
          <h2>
            {metricLabel} — últimos {days} dias
          </h2>
          <div className="tabs" style={{ marginBottom: 0 }}>
            {METRICS.map((item) => (
              <button
                key={item.key}
                type="button"
                className={metric === item.key ? "active" : ""}
                onClick={() => setMetric(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
        <ComparisonChart
          points={statsQuery.data?.points ?? []}
          previous={previousStatsQuery.data?.points ?? []}
          metric={metric}
        />
      </div>

      <div className="grid cols-2">
        <div className="panel">
          <h2>Principais páginas</h2>
          {pagesQuery.isLoading ? (
            <p className="muted">Carregando...</p>
          ) : (
            <HorizontalBarList items={pagesQuery.data?.items ?? []} metric="pageviews" />
          )}
        </div>
        <div className="panel">
          <h2>Referrers</h2>
          {referrersQuery.isLoading ? (
            <p className="muted">Carregando...</p>
          ) : (
            <HorizontalBarList items={referrersQuery.data?.items ?? []} metric="visitors" />
          )}
        </div>
      </div>

      <div className="panel">
        <h2>Países</h2>
        <div className="map-layout">
          <div className="map-canvas">
            <GeoMap data={geoQuery.data ?? []} />
          </div>
          <div className="geo-legend">
            {(geoQuery.data ?? []).slice(0, 6).map((item, index) => (
              <div className="geo-legend-row" key={item.country}>
                <span className="geo-rank">{index + 1}</span>
                <span className="geo-country">{item.country}</span>
                <span className="geo-visitors">{formatNumber(item.visitors)}</span>
                <span className="geo-percent muted">
                  {geoTotal > 0 ? `${Math.round((item.visitors / geoTotal) * 100)}%` : "0%"}
                </span>
              </div>
            ))}
            {!geoQuery.data?.length ? <p className="muted">Sem dados no período.</p> : null}
          </div>
        </div>
      </div>

      <div className="grid cols-2">
        <div className="panel">
          <h2>Dispositivos</h2>
          {devicesQuery.isLoading ? (
            <p className="muted">Carregando...</p>
          ) : (
            <DonutChart items={devicesQuery.data?.items ?? []} />
          )}
        </div>
        <div className="panel">
          <h2>Navegadores</h2>
          {browsersQuery.isLoading ? (
            <p className="muted">Carregando...</p>
          ) : (
            <DimensionTimeseriesChart data={browsersQuery.data} metric="visitors" />
          )}
        </div>
      </div>

      <div className="panel">
        <h2>Fluxo de usuários — Origem → Página de entrada → Página de saída</h2>
        {flowQuery.isLoading ? (
          <p className="muted">Carregando...</p>
        ) : (
          <UserFlowChart data={flowQuery.data} />
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

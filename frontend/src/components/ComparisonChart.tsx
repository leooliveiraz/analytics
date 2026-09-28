import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { StatsPoint } from "../api/types";
import { formatBucket } from "../lib/format";

type Metric = "pageviews" | "visitors" | "sessions";

interface ComparisonChartProps {
  points: StatsPoint[];
  previous?: StatsPoint[];
  metric: Metric;
}

export function ComparisonChart({ points, previous = [], metric }: ComparisonChartProps) {
  const data = points.map((point, index) => ({
    label: formatBucket(point.bucket),
    current: point[metric],
    previous: previous[index]?.[metric] ?? null,
  }));

  if (!data.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <ComposedChart data={data} margin={{ top: 10, right: 20, bottom: 0, left: 0 }}>
        <defs>
          <linearGradient id="comparisonFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#6ea8fe" stopOpacity={0.35} />
            <stop offset="100%" stopColor="#6ea8fe" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#2a2f3a" vertical={false} />
        <XAxis dataKey="label" tick={{ fill: "#8b93a7", fontSize: 12 }} minTickGap={24} />
        <YAxis tick={{ fill: "#8b93a7", fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: "#171a21", border: "1px solid #2a2f3a", borderRadius: 8 }}
          labelStyle={{ color: "#e6e9ef" }}
        />
        <Legend wrapperStyle={{ color: "#8b93a7" }} />
        <Area
          type="monotone"
          dataKey="current"
          stroke="#6ea8fe"
          strokeWidth={2}
          fill="url(#comparisonFill)"
          name="Atual"
        />
        <Line
          type="monotone"
          dataKey="previous"
          stroke="#8b93a7"
          strokeWidth={2}
          strokeDasharray="4 4"
          dot={false}
          name="Anterior"
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}

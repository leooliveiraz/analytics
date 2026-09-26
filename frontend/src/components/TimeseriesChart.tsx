import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { StatsPoint } from "../api/types";
import { formatBucket } from "../lib/format";

interface TimeseriesChartProps {
  points: StatsPoint[];
}

export function TimeseriesChart({ points }: TimeseriesChartProps) {
  const data = points.map((point) => ({
    ...point,
    label: formatBucket(point.bucket),
  }));

  if (!data.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} margin={{ top: 10, right: 20, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#2a2f3a" />
        <XAxis dataKey="label" tick={{ fill: "#8b93a7", fontSize: 12 }} minTickGap={24} />
        <YAxis tick={{ fill: "#8b93a7", fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: "#171a21", border: "1px solid #2a2f3a", borderRadius: 8 }}
          labelStyle={{ color: "#e6e9ef" }}
        />
        <Legend wrapperStyle={{ color: "#8b93a7" }} />
        <Line type="monotone" dataKey="visitors" stroke="#6ea8fe" strokeWidth={2} dot={false} name="Visitantes" />
        <Line type="monotone" dataKey="pageviews" stroke="#7ee787" strokeWidth={2} dot={false} name="Pageviews" />
      </LineChart>
    </ResponsiveContainer>
  );
}

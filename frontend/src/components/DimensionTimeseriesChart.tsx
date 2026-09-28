import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { DimensionTimeseriesResponse } from "../api/types";
import { formatBucket } from "../lib/format";
import { CHART_COLORS } from "../lib/palette";

interface DimensionTimeseriesChartProps {
  data?: DimensionTimeseriesResponse;
  metric?: "visitors" | "pageviews";
}

export function DimensionTimeseriesChart({ data, metric = "visitors" }: DimensionTimeseriesChartProps) {
  if (!data || !data.series.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  const buckets = Array.from(
    new Set(data.series.flatMap((item) => item.points.map((point) => point.bucket))),
  ).sort();

  const rows = buckets.map((bucket) => {
    const row: Record<string, string | number> = { label: formatBucket(bucket) };
    data.series.forEach((item, index) => {
      const point = item.points.find((candidate) => candidate.bucket === bucket);
      row[`s${index}`] = point ? point[metric] : 0;
    });
    return row;
  });

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={rows} margin={{ top: 10, right: 20, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#2a2f3a" vertical={false} />
        <XAxis dataKey="label" tick={{ fill: "#8b93a7", fontSize: 12 }} minTickGap={24} />
        <YAxis tick={{ fill: "#8b93a7", fontSize: 12 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{
            background: "#171a21",
            border: "1px solid #2a2f3a",
            borderRadius: 8,
            color: "#e6e9ef",
          }}
          labelStyle={{ color: "#e6e9ef" }}
          itemStyle={{ color: "#e6e9ef" }}
        />
        <Legend wrapperStyle={{ color: "#8b93a7" }} />
        {data.series.map((item, index) => (
          <Line
            key={item.value}
            type="monotone"
            dataKey={`s${index}`}
            name={item.value}
            stroke={CHART_COLORS[index % CHART_COLORS.length]}
            strokeWidth={2}
            dot={false}
          />
        ))}
      </LineChart>
    </ResponsiveContainer>
  );
}

import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { BreakdownItem } from "../api/types";
import { formatNumber } from "../lib/format";
import { CHART_COLORS } from "../lib/palette";

interface DonutChartProps {
  items: BreakdownItem[];
}

export function DonutChart({ items }: DonutChartProps) {
  const data = items.map((item) => ({ name: item.value, value: item.visitors }));
  const total = data.reduce((sum, item) => sum + item.value, 0);

  if (!data.length || total === 0) {
    return <p className="muted">Sem dados no período.</p>;
  }

  return (
    <div className="donut-wrap">
      <ResponsiveContainer width="100%" height={260}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius={72}
            outerRadius={100}
            paddingAngle={2}
            stroke="none"
          >
            {data.map((item, index) => (
              <Cell key={item.name} fill={CHART_COLORS[index % CHART_COLORS.length]} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              background: "#171a21",
              border: "1px solid #2a2f3a",
              borderRadius: 8,
              color: "#e6e9ef",
            }}
            itemStyle={{ color: "#e6e9ef" }}
            formatter={(value: number) => formatNumber(value)}
          />
        </PieChart>
      </ResponsiveContainer>
      <div className="donut-center">
        <span className="muted">Total</span>
        <strong>{formatNumber(total)}</strong>
      </div>
      <div className="donut-legend">
        {data.map((item, index) => (
          <span key={item.name} className="donut-legend-item">
            <span
              className="legend-dot"
              style={{ background: CHART_COLORS[index % CHART_COLORS.length] }}
            />
            {item.name}
          </span>
        ))}
      </div>
    </div>
  );
}

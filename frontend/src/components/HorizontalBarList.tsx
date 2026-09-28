import type { BreakdownItem } from "../api/types";
import { formatNumber } from "../lib/format";

interface HorizontalBarListProps {
  items: BreakdownItem[];
  metric?: "visitors" | "pageviews";
}

export function HorizontalBarList({ items, metric = "visitors" }: HorizontalBarListProps) {
  if (!items.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  const max = Math.max(1, ...items.map((item) => item[metric]));

  return (
    <div className="bar-list">
      {items.map((item) => {
        const value = item[metric];
        const width = (value / max) * 100;
        return (
          <div className="bar-row" key={item.value}>
            <div className="bar-head">
              <span className="bar-label" title={item.value}>
                {item.value}
              </span>
              <span className="bar-value">{formatNumber(value)}</span>
            </div>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${width}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

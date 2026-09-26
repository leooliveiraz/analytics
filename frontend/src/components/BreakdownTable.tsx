import type { BreakdownItem } from "../api/types";
import { formatNumber } from "../lib/format";

interface BreakdownTableProps {
  items: BreakdownItem[];
}

export function BreakdownTable({ items }: BreakdownTableProps) {
  if (!items.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  return (
    <table className="table">
      <thead>
        <tr>
          <th>Valor</th>
          <th className="right">Visitantes</th>
          <th className="right">Pageviews</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.value}>
            <td className="ellipsis" title={item.value}>
              {item.value}
            </td>
            <td className="right">{formatNumber(item.visitors)}</td>
            <td className="right">{formatNumber(item.pageviews)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

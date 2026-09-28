interface StatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  accent?: boolean;
  delta?: number | null;
}

function DeltaBadge({ value }: { value: number }) {
  if (value === 0) {
    return <span className="stat-delta muted">0%</span>;
  }
  const up = value > 0;
  return (
    <span className={`stat-delta ${up ? "up" : "down"}`}>
      {up ? "▲" : "▼"} {Math.abs(value * 100).toFixed(0)}%
    </span>
  );
}

export function StatCard({ label, value, sub, accent, delta }: StatCardProps) {
  return (
    <div className="stat-card">
      <div className="stat-label">
        {accent ? <span className="stat-dot" /> : null}
        {label}
      </div>
      <div className="stat-value">{value}</div>
      <div className="stat-sub">
        {delta !== undefined && delta !== null ? <DeltaBadge value={delta} /> : null}
        {sub ? <span className="muted">{sub}</span> : null}
      </div>
    </div>
  );
}

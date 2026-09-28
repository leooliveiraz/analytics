import { RANGES } from "../lib/useRange";

interface RangeTabsProps {
  days: number;
  onChange: (days: number) => void;
}

export function RangeTabs({ days, onChange }: RangeTabsProps) {
  return (
    <div className="tabs">
      {RANGES.map((range) => (
        <button
          key={range.days}
          type="button"
          className={days === range.days ? "active" : ""}
          onClick={() => onChange(range.days)}
        >
          {range.label}
        </button>
      ))}
    </div>
  );
}

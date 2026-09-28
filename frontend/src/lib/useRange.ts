import { useState } from "react";
import { daysAgo, today } from "./format";

export const RANGES = [
  { days: 7, label: "7 dias" },
  { days: 30, label: "30 dias" },
  { days: 90, label: "90 dias" },
];

export function useRange(initial = 7) {
  const [days, setDays] = useState(initial);
  return { days, setDays, from: daysAgo(days - 1), to: today() };
}

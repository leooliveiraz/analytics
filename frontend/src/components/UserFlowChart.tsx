import { ResponsiveContainer, Sankey, Tooltip } from "recharts";
import type { FlowResponse } from "../api/types";
import { FLOW_COLORS } from "../lib/palette";

interface UserFlowChartProps {
  data?: FlowResponse;
}

interface SankeyNodeProps {
  x: number;
  y: number;
  width: number;
  height: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  payload: any;
}

interface SankeyLinkProps {
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  sourceControlX: number;
  targetControlX: number;
  linkWidth: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  payload: any;
}

function renderNode({ x, y, width, height, payload }: SankeyNodeProps) {
  const fill = FLOW_COLORS[payload.column] ?? "#6ea8fe";
  const alignRight = payload.column === "exit";
  return (
    <g>
      <rect x={x} y={y} width={Math.max(width, 2)} height={Math.max(height, 1)} rx={2} fill={fill} />
      <text
        x={alignRight ? x - 6 : x + width + 6}
        y={y + height / 2}
        dy={4}
        textAnchor={alignRight ? "end" : "start"}
        fill="#c9d1e0"
        fontSize={12}
      >
        {payload.label}
      </text>
    </g>
  );
}

function renderLink({
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourceControlX,
  targetControlX,
  linkWidth,
  payload,
}: SankeyLinkProps) {
  const color = FLOW_COLORS[payload.source.column] ?? "#6ea8fe";
  const d = `M${sourceX},${sourceY}C${sourceControlX},${sourceY} ${targetControlX},${targetY} ${targetX},${targetY}`;
  return <path d={d} fill="none" stroke={color} strokeWidth={linkWidth} strokeOpacity={0.25} />;
}

export function UserFlowChart({ data }: UserFlowChartProps) {
  if (!data || !data.links.length) {
    return <p className="muted">Sem dados no período.</p>;
  }

  const indexById = new Map(data.nodes.map((node, index) => [node.id, index]));
  const sankeyData = {
    nodes: data.nodes.map((node) => ({
      name: node.label,
      label: node.label,
      column: node.column,
      visitors: node.visitors,
    })),
    links: data.links
      .filter((link) => indexById.has(link.source) && indexById.has(link.target))
      .map((link) => ({
        source: indexById.get(link.source) as number,
        target: indexById.get(link.target) as number,
        value: link.visitors,
      })),
  };

  return (
    <ResponsiveContainer width="100%" height={360}>
      <Sankey
        data={sankeyData}
        nodePadding={24}
        nodeWidth={8}
        linkCurvature={0.5}
        node={renderNode}
        link={renderLink}
        margin={{ top: 10, right: 140, bottom: 10, left: 140 }}
      >
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
      </Sankey>
    </ResponsiveContainer>
  );
}

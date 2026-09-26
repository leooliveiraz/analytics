export interface User {
  id: string;
  email: string;
  name: string | null;
  createdAt: string;
}

export interface Tokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export type Role = "OWNER" | "ADMIN" | "VIEWER";

export interface Project {
  id: string;
  name: string;
  domain: string | null;
  timezone: string;
  publicKey: string;
  role: Role;
  createdAt: string;
}

export interface Member {
  userId: string;
  email: string;
  name: string | null;
  role: Role;
  createdAt: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface ApiKeyCreated extends ApiKey {
  apiKey: string;
}

export interface Overview {
  visitors: number;
  pageviews: number;
  sessions: number;
  bounces: number;
  bounceRate: number;
  avgDurationSeconds: number;
  pageviewsPerSession: number;
}

export interface StatsPoint {
  bucket: string;
  visitors: number;
  pageviews: number;
  sessions: number;
  bounces: number;
}

export interface StatsResponse {
  interval: string;
  from: string;
  to: string;
  points: StatsPoint[];
}

export interface BreakdownItem {
  value: string;
  visitors: number;
  pageviews: number;
}

export interface BreakdownResponse {
  dimension: string;
  items: BreakdownItem[];
}

export interface RealtimePoint {
  bucket: string;
  pageviews: number;
}

export interface RealtimeResponse {
  activeVisitors: number;
  points: RealtimePoint[];
}

export interface EventRow {
  id: string;
  eventName: string;
  path: string | null;
  url: string | null;
  referrerDomain: string | null;
  country: string | null;
  deviceType: string | null;
  browser: string | null;
  os: string | null;
  visitorId: string | null;
  sessionId: string | null;
  occurredAt: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

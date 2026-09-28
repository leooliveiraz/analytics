package com.analytics.analytics.dto

import java.time.Instant

data class StatsPoint(
    val bucket: String,
    val visitors: Long,
    val pageviews: Long,
    val sessions: Long,
    val bounces: Long,
)

data class StatsResponse(
    val interval: String,
    val from: String,
    val to: String,
    val points: List<StatsPoint>,
)

data class BreakdownItem(
    val value: String,
    val visitors: Long,
    val pageviews: Long,
)

data class BreakdownResponse(
    val dimension: String,
    val items: List<BreakdownItem>,
)

data class OverviewResponse(
    val visitors: Long,
    val pageviews: Long,
    val sessions: Long,
    val bounces: Long,
    val bounceRate: Double,
    val avgDurationSeconds: Double,
    val pageviewsPerSession: Double,
)

data class RealtimePoint(
    val bucket: String,
    val pageviews: Long,
)

data class RealtimeResponse(
    val activeVisitors: Long,
    val points: List<RealtimePoint>,
)

data class EventRow(
    val id: String,
    val eventName: String,
    val path: String?,
    val url: String?,
    val referrerDomain: String?,
    val country: String?,
    val deviceType: String?,
    val browser: String?,
    val os: String?,
    val visitorId: String?,
    val sessionId: String?,
    val occurredAt: Instant,
)

data class SessionRow(
    val id: String,
    val visitorId: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val durationSeconds: Int,
    val pageviews: Int,
    val entryPath: String?,
    val exitPath: String?,
    val isBounce: Boolean,
    val country: String?,
    val deviceType: String?,
    val browser: String?,
    val os: String?,
    val referrerDomain: String?,
)

data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)

data class PageMetricRow(
    val path: String,
    val pageviews: Long,
    val visitors: Long,
    val entries: Long,
    val avgTimeMs: Double,
    val avgScrollPct: Double,
)

data class ElementStatRow(
    val selector: String,
    val eventName: String,
    val text: String?,
    val tag: String?,
    val clicks: Long,
    val visitors: Long,
    val sessions: Long,
)

data class ImageStatRow(
    val imageKey: String,
    val imageAlt: String?,
    val impressions: Long,
    val sessions: Long,
    val avgDwellMs: Double,
)

data class SectionStatRow(
    val sectionKey: String,
    val views: Long,
    val visitors: Long,
    val avgDwellMs: Double,
)

data class HeatmapPoint(
    val x: Double,
    val y: Double,
    val weight: Long,
)

data class HeatmapResponse(
    val type: String,
    val path: String?,
    val maxWeight: Long,
    val points: List<HeatmapPoint>,
)

data class GeoStatItem(
    val country: String,
    val visitors: Long,
    val pageviews: Long,
)

data class SessionDetailResponse(
    val session: SessionRow,
    val events: List<EventRow>,
)


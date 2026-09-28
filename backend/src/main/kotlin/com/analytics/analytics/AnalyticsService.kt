package com.analytics.analytics

import com.analytics.analytics.dto.BreakdownResponse
import com.analytics.analytics.dto.ElementStatRow
import com.analytics.analytics.dto.EventRow
import com.analytics.analytics.dto.GeoStatItem
import com.analytics.analytics.dto.HeatmapResponse
import com.analytics.analytics.dto.ImageStatRow
import com.analytics.analytics.dto.OverviewResponse
import com.analytics.analytics.dto.PageMetricRow
import com.analytics.analytics.dto.PageResponse
import com.analytics.analytics.dto.RealtimeResponse
import com.analytics.analytics.dto.SectionStatRow
import com.analytics.analytics.dto.SessionDetailResponse
import com.analytics.analytics.dto.SessionRow
import com.analytics.analytics.dto.StatsResponse
import com.analytics.common.exception.BadRequestException
import com.analytics.common.exception.NotFoundException
import com.analytics.project.ProjectService
import com.analytics.project.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository,
    private val projectService: ProjectService,
) {

    @Transactional(readOnly = true)
    fun stats(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        interval: String,
    ): StatsResponse {
        val zone = timezone(projectId, userId, from, to)
        val fromInstant = startOfDay(from, zone)
        val toInstant = startOfDay(to.plusDays(1), zone)
        val safeInterval = validateInterval(interval)
        val points = analyticsRepository.eventStats(projectId, fromInstant, toInstant, zone.id, safeInterval)
        val bounces = analyticsRepository.sessionBounces(projectId, fromInstant, toInstant, zone.id, safeInterval)
        return StatsResponse(
            interval = safeInterval,
            from = from.toString(),
            to = to.toString(),
            points = points.map { it.copy(bounces = bounces[it.bucket] ?: 0L) },
        )
    }

    @Transactional(readOnly = true)
    fun breakdown(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        dimension: String,
        limit: Int,
    ): BreakdownResponse {
        val zone = timezone(projectId, userId, from, to)
        val expression = DIMENSIONS[dimension.lowercase()]
            ?: throw BadRequestException("Unsupported dimension: $dimension")
        val items = analyticsRepository.breakdown(
            projectId,
            startOfDay(from, zone),
            startOfDay(to.plusDays(1), zone),
            expression,
            limit.coerceIn(1, 100),
        )
        return BreakdownResponse(dimension, items)
    }

    @Transactional(readOnly = true)
    fun overview(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): OverviewResponse {
        val zone = timezone(projectId, userId, from, to)
        val fromInstant = startOfDay(from, zone)
        val toInstant = startOfDay(to.plusDays(1), zone)
        val (visitors, pageviews, eventSessions) = analyticsRepository.eventTotals(projectId, fromInstant, toInstant)
        val (sessionCount, bounces, duration) = analyticsRepository.sessionTotals(projectId, fromInstant, toInstant)
        val sessions = maxOf(eventSessions, sessionCount)
        val bounceRate = if (sessions > 0) bounces.toDouble() / sessions else 0.0
        val avgDuration = if (sessions > 0) duration.toDouble() / sessions else 0.0
        val pageviewsPerSession = if (sessions > 0) pageviews.toDouble() / sessions else 0.0
        return OverviewResponse(
            visitors = visitors,
            pageviews = pageviews,
            sessions = sessions,
            bounces = bounces,
            bounceRate = round2(bounceRate),
            avgDurationSeconds = round2(avgDuration),
            pageviewsPerSession = round2(pageviewsPerSession),
        )
    }

    @Transactional(readOnly = true)
    fun realtime(projectId: UUID, userId: UUID, minutes: Int): RealtimeResponse {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        val zone = zoneOf(projectService.findProject(projectId).timezone)
        val from = Instant.now().minus(minutes.coerceIn(1, 1440).toLong(), ChronoUnit.MINUTES)
        return RealtimeResponse(
            activeVisitors = analyticsRepository.activeVisitors(projectId, from),
            points = analyticsRepository.realtimeSeries(projectId, from, zone.id),
        )
    }

    @Transactional(readOnly = true)
    fun events(
        projectId: UUID,
        userId: UUID,
        from: Instant?,
        to: Instant?,
        eventName: String?,
        path: String?,
        page: Int,
        size: Int,
    ): PageResponse<EventRow> {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        val effectiveTo = to ?: Instant.now()
        val effectiveFrom = from ?: effectiveTo.minus(7, ChronoUnit.DAYS)
        return analyticsRepository.events(
            projectId,
            effectiveFrom,
            effectiveTo,
            eventName,
            path,
            page.coerceAtLeast(0),
            size.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun sessions(
        projectId: UUID,
        userId: UUID,
        from: Instant?,
        to: Instant?,
        page: Int,
        size: Int,
    ): PageResponse<SessionRow> {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        val effectiveTo = to ?: Instant.now()
        val effectiveFrom = from ?: effectiveTo.minus(30, ChronoUnit.DAYS)
        return analyticsRepository.sessions(
            projectId,
            effectiveFrom,
            effectiveTo,
            page.coerceAtLeast(0),
            size.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun pages(projectId: UUID, userId: UUID, from: LocalDate, to: LocalDate, limit: Int): List<PageMetricRow> {
        val zone = timezone(projectId, userId, from, to)
        return analyticsRepository.pages(
            projectId,
            startOfDay(from, zone),
            startOfDay(to.plusDays(1), zone),
            limit.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun elements(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        path: String?,
        limit: Int,
    ): List<ElementStatRow> {
        val zone = timezone(projectId, userId, from, to)
        return analyticsRepository.elements(
            projectId,
            startOfDay(from, zone),
            startOfDay(to.plusDays(1), zone),
            path?.takeIf { it.isNotBlank() },
            limit.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun images(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        path: String?,
        limit: Int,
    ): List<ImageStatRow> {
        val zone = timezone(projectId, userId, from, to)
        return analyticsRepository.images(
            projectId,
            startOfDay(from, zone),
            startOfDay(to.plusDays(1), zone),
            path?.takeIf { it.isNotBlank() },
            limit.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun sections(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        path: String?,
        limit: Int,
    ): List<SectionStatRow> {
        val zone = timezone(projectId, userId, from, to)
        return analyticsRepository.sections(
            projectId,
            startOfDay(from, zone),
            startOfDay(to.plusDays(1), zone),
            path?.takeIf { it.isNotBlank() },
            limit.coerceIn(1, 200),
        )
    }

    @Transactional(readOnly = true)
    fun heatmap(
        projectId: UUID,
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        type: String,
        path: String?,
        device: String?,
        limit: Int,
    ): HeatmapResponse {
        val zone = timezone(projectId, userId, from, to)
        val normalized = type.lowercase()
        val scroll = normalized == "scroll"
        val eventName = when (normalized) {
            "click" -> "click"
            "move" -> "mousemove"
            "scroll" -> "pageleave"
            else -> throw BadRequestException("Unsupported heatmap type: $type")
        }
        val fromInstant = startOfDay(from, zone)
        val toInstant = startOfDay(to.plusDays(1), zone)
        val cleanPath = path?.takeIf { it.isNotBlank() }
        val cleanDevice = device?.takeIf { it.isNotBlank() }
        val points = analyticsRepository.heatmap(
            projectId,
            fromInstant,
            toInstant,
            eventName,
            cleanPath,
            cleanDevice,
            scroll,
            limit.coerceIn(100, 20000),
        )
        val viewportWidth = analyticsRepository.representativeViewportWidth(
            projectId,
            fromInstant,
            toInstant,
            cleanPath,
            cleanDevice,
        )
        return HeatmapResponse(
            type = normalized,
            path = path,
            maxWeight = points.maxOfOrNull { it.weight } ?: 0L,
            viewportWidth = viewportWidth,
            pageHeight = analyticsRepository.maxPageHeight(
                projectId,
                fromInstant,
                toInstant,
                cleanPath,
                cleanDevice,
                viewportWidth,
            ),
            points = points,
        )
    }

    @Transactional(readOnly = true)
    fun geo(projectId: UUID, userId: UUID, from: LocalDate, to: LocalDate): List<GeoStatItem> {
        val zone = timezone(projectId, userId, from, to)
        return analyticsRepository.geo(projectId, startOfDay(from, zone), startOfDay(to.plusDays(1), zone))
    }

    @Transactional(readOnly = true)
    fun sessionDetail(projectId: UUID, userId: UUID, sessionId: UUID): SessionDetailResponse {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        val session = analyticsRepository.findSession(projectId, sessionId)
            ?: throw NotFoundException("Session not found")
        val events = analyticsRepository.eventsForSession(projectId, sessionId, 500)
        return SessionDetailResponse(session, events)
    }

    private fun timezone(projectId: UUID, userId: UUID, from: LocalDate, to: LocalDate): ZoneId {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        if (to.isBefore(from)) {
            throw BadRequestException("'to' must not be before 'from'")
        }
        return zoneOf(projectService.findProject(projectId).timezone)
    }

    private fun startOfDay(date: LocalDate, zone: ZoneId): Instant = date.atStartOfDay(zone).toInstant()

    private fun zoneOf(timezone: String): ZoneId =
        runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)

    private fun validateInterval(interval: String): String {
        val normalized = interval.lowercase()
        if (normalized !in SUPPORTED_INTERVALS) {
            throw BadRequestException("interval must be one of $SUPPORTED_INTERVALS")
        }
        return normalized
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

    companion object {
        private val SUPPORTED_INTERVALS = setOf("day", "hour", "minute")

        private val DIMENSIONS = mapOf(
            "path" to "COALESCE(NULLIF(path, ''), '(unknown)')",
            "referrer" to "COALESCE(NULLIF(referrer_domain, ''), 'direct')",
            "country" to "COALESCE(NULLIF(country, ''), 'unknown')",
            "device" to "COALESCE(NULLIF(device_type, ''), 'unknown')",
            "browser" to "COALESCE(NULLIF(browser, ''), 'unknown')",
            "os" to "COALESCE(NULLIF(os, ''), 'unknown')",
            "utm_source" to "COALESCE(NULLIF(utm_source, ''), 'none')",
            "event" to "event_name",
        )
    }
}

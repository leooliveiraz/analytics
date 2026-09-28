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
import com.analytics.security.SecurityUtils
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class AnalyticsController(private val analyticsService: AnalyticsService) {

    @GetMapping("/overview")
    fun overview(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): OverviewResponse = analyticsService.overview(projectId, SecurityUtils.currentUserId(), from, to)

    @GetMapping("/stats")
    fun stats(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(defaultValue = "day") interval: String,
    ): StatsResponse = analyticsService.stats(projectId, SecurityUtils.currentUserId(), from, to, interval)

    @GetMapping("/breakdown")
    fun breakdown(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam dimension: String,
        @RequestParam(defaultValue = "10") limit: Int,
    ): BreakdownResponse =
        analyticsService.breakdown(projectId, SecurityUtils.currentUserId(), from, to, dimension, limit)

    @GetMapping("/realtime")
    fun realtime(
        @PathVariable projectId: UUID,
        @RequestParam(defaultValue = "30") minutes: Int,
    ): RealtimeResponse = analyticsService.realtime(projectId, SecurityUtils.currentUserId(), minutes)

    @GetMapping("/events")
    fun events(
        @PathVariable projectId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(required = false) eventName: String?,
        @RequestParam(required = false) path: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<EventRow> =
        analyticsService.events(projectId, SecurityUtils.currentUserId(), from, to, eventName, path, page, size)

    @GetMapping("/sessions")
    fun sessions(
        @PathVariable projectId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<SessionRow> =
        analyticsService.sessions(projectId, SecurityUtils.currentUserId(), from, to, page, size)

    @GetMapping("/pages")
    fun pages(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<PageMetricRow> =
        analyticsService.pages(projectId, SecurityUtils.currentUserId(), from, to, limit)

    @GetMapping("/elements")
    fun elements(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) path: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<ElementStatRow> =
        analyticsService.elements(projectId, SecurityUtils.currentUserId(), from, to, path, limit)

    @GetMapping("/images")
    fun images(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) path: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<ImageStatRow> =
        analyticsService.images(projectId, SecurityUtils.currentUserId(), from, to, path, limit)

    @GetMapping("/sections")
    fun sections(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) path: String?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<SectionStatRow> =
        analyticsService.sections(projectId, SecurityUtils.currentUserId(), from, to, path, limit)

    @GetMapping("/heatmap")
    fun heatmap(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(defaultValue = "click") type: String,
        @RequestParam(required = false) path: String?,
        @RequestParam(required = false) device: String?,
        @RequestParam(defaultValue = "5000") limit: Int,
    ): HeatmapResponse =
        analyticsService.heatmap(projectId, SecurityUtils.currentUserId(), from, to, type, path, device, limit)

    @GetMapping("/geo")
    fun geo(
        @PathVariable projectId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<GeoStatItem> =
        analyticsService.geo(projectId, SecurityUtils.currentUserId(), from, to)

    @GetMapping("/sessions/{sessionId}")
    fun sessionDetail(
        @PathVariable projectId: UUID,
        @PathVariable sessionId: UUID,
    ): SessionDetailResponse =
        analyticsService.sessionDetail(projectId, SecurityUtils.currentUserId(), sessionId)
}

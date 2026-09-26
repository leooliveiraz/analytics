package com.analytics.analytics

import com.analytics.analytics.dto.BreakdownResponse
import com.analytics.analytics.dto.EventRow
import com.analytics.analytics.dto.OverviewResponse
import com.analytics.analytics.dto.PageResponse
import com.analytics.analytics.dto.RealtimeResponse
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
}

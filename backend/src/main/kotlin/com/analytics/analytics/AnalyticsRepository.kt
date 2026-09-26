package com.analytics.analytics

import com.analytics.analytics.dto.BreakdownItem
import com.analytics.analytics.dto.EventRow
import com.analytics.analytics.dto.PageResponse
import com.analytics.analytics.dto.RealtimePoint
import com.analytics.analytics.dto.SessionRow
import com.analytics.analytics.dto.StatsPoint
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class AnalyticsRepository(private val jdbc: NamedParameterJdbcTemplate) {

    fun eventStats(projectId: java.util.UUID, from: Instant, to: Instant, timezone: String, interval: String): List<StatsPoint> {
        val sql = """
            SELECT to_char(date_trunc(:interval, occurred_at AT TIME ZONE :tz), 'YYYY-MM-DD"T"HH24:MI:SS') AS bucket,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(*) FILTER (WHERE event_name = 'pageview') AS pageviews,
                   COUNT(DISTINCT session_id) AS sessions
            FROM events
            WHERE project_id = :projectId
              AND occurred_at >= :from AND occurred_at < :to
            GROUP BY bucket
            ORDER BY bucket
        """.trimIndent()
        val params = timeParams(projectId, from, to)
            .addValue("tz", timezone)
            .addValue("interval", interval)
        return jdbc.query(sql, params) { rs, _ ->
            StatsPoint(
                bucket = rs.getString("bucket"),
                visitors = rs.getLong("visitors"),
                pageviews = rs.getLong("pageviews"),
                sessions = rs.getLong("sessions"),
                bounces = 0,
            )
        }
    }

    fun sessionBounces(projectId: java.util.UUID, from: Instant, to: Instant, timezone: String, interval: String): Map<String, Long> {
        val sql = """
            SELECT to_char(date_trunc(:interval, started_at AT TIME ZONE :tz), 'YYYY-MM-DD"T"HH24:MI:SS') AS bucket,
                   COUNT(*) FILTER (WHERE is_bounce) AS bounces
            FROM sessions
            WHERE project_id = :projectId
              AND started_at >= :from AND started_at < :to
            GROUP BY bucket
        """.trimIndent()
        val params = timeParams(projectId, from, to)
            .addValue("tz", timezone)
            .addValue("interval", interval)
        return jdbc.query(sql, params) { rs, _ -> rs.getString("bucket") to rs.getLong("bounces") }
            .toMap()
    }

    fun breakdown(projectId: java.util.UUID, from: Instant, to: Instant, dimensionExpression: String, limit: Int): List<BreakdownItem> {
        val sql = """
            SELECT $dimensionExpression AS value,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(*) FILTER (WHERE event_name = 'pageview') AS pageviews
            FROM events
            WHERE project_id = :projectId
              AND occurred_at >= :from AND occurred_at < :to
            GROUP BY value
            ORDER BY pageviews DESC, visitors DESC
            LIMIT :limit
        """.trimIndent()
        val params = timeParams(projectId, from, to).addValue("limit", limit)
        return jdbc.query(sql, params) { rs, _ ->
            BreakdownItem(
                value = rs.getString("value") ?: "unknown",
                visitors = rs.getLong("visitors"),
                pageviews = rs.getLong("pageviews"),
            )
        }
    }

    fun eventTotals(projectId: java.util.UUID, from: Instant, to: Instant): Triple<Long, Long, Long> {
        val sql = """
            SELECT COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(*) FILTER (WHERE event_name = 'pageview') AS pageviews,
                   COUNT(DISTINCT session_id) AS sessions
            FROM events
            WHERE project_id = :projectId AND occurred_at >= :from AND occurred_at < :to
        """.trimIndent()
        val row = jdbc.query(sql, timeParams(projectId, from, to)) { rs, _ ->
            Triple(rs.getLong("visitors"), rs.getLong("pageviews"), rs.getLong("sessions"))
        }.firstOrNull()
        return row ?: Triple(0, 0, 0)
    }

    fun sessionTotals(projectId: java.util.UUID, from: Instant, to: Instant): Triple<Long, Long, Long> {
        val sql = """
            SELECT COUNT(*) AS sessions,
                   COUNT(*) FILTER (WHERE is_bounce) AS bounces,
                   COALESCE(SUM(duration_seconds), 0) AS duration
            FROM sessions
            WHERE project_id = :projectId AND started_at >= :from AND started_at < :to
        """.trimIndent()
        val row = jdbc.query(sql, timeParams(projectId, from, to)) { rs, _ ->
            Triple(rs.getLong("sessions"), rs.getLong("bounces"), rs.getLong("duration"))
        }.firstOrNull()
        return row ?: Triple(0, 0, 0)
    }

    fun realtimeSeries(projectId: java.util.UUID, from: Instant, timezone: String): List<RealtimePoint> {
        val sql = """
            SELECT to_char(date_trunc('minute', occurred_at AT TIME ZONE :tz), 'YYYY-MM-DD"T"HH24:MI:SS') AS bucket,
                   COUNT(*) AS pageviews
            FROM events
            WHERE project_id = :projectId AND occurred_at >= :from AND event_name = 'pageview'
            GROUP BY bucket
            ORDER BY bucket
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("from", Timestamp.from(from))
            .addValue("tz", timezone)
        return jdbc.query(sql, params) { rs, _ ->
            RealtimePoint(rs.getString("bucket"), rs.getLong("pageviews"))
        }
    }

    fun activeVisitors(projectId: java.util.UUID, from: Instant): Long {
        val sql = """
            SELECT COUNT(DISTINCT visitor_id) AS active
            FROM events
            WHERE project_id = :projectId AND occurred_at >= :from
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("from", Timestamp.from(from))
        return jdbc.query(sql, params) { rs, _ -> rs.getLong("active") }.firstOrNull() ?: 0
    }

    fun events(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        eventName: String?,
        path: String?,
        page: Int,
        size: Int,
    ): PageResponse<EventRow> {
        val conditions = StringBuilder("project_id = :projectId AND occurred_at >= :from AND occurred_at < :to")
        val params = timeParams(projectId, from, to)
        eventName?.let {
            conditions.append(" AND event_name = :eventName")
            params.addValue("eventName", it)
        }
        path?.let {
            conditions.append(" AND path = :path")
            params.addValue("path", it)
        }
        val total = jdbc.query(
            "SELECT COUNT(*) AS total FROM events WHERE $conditions",
            params,
        ) { rs, _ -> rs.getLong("total") }.firstOrNull() ?: 0

        val items = jdbc.query(
            """
            SELECT id, event_name, path, url, referrer_domain, country, device_type, browser, os,
                   visitor_id, session_id, occurred_at
            FROM events
            WHERE $conditions
            ORDER BY occurred_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params.addValue("limit", size).addValue("offset", page * size),
        ) { rs, _ ->
            EventRow(
                id = rs.getString("id"),
                eventName = rs.getString("event_name"),
                path = rs.getString("path"),
                url = rs.getString("url"),
                referrerDomain = rs.getString("referrer_domain"),
                country = rs.getString("country"),
                deviceType = rs.getString("device_type"),
                browser = rs.getString("browser"),
                os = rs.getString("os"),
                visitorId = rs.getString("visitor_id"),
                sessionId = rs.getString("session_id"),
                occurredAt = rs.getTimestamp("occurred_at").toInstant(),
            )
        }
        return PageResponse(items, page, size, total)
    }

    fun sessions(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        page: Int,
        size: Int,
    ): PageResponse<SessionRow> {
        val conditions = "project_id = :projectId AND started_at >= :from AND started_at < :to"
        val params = timeParams(projectId, from, to)
        val total = jdbc.query(
            "SELECT COUNT(*) AS total FROM sessions WHERE $conditions",
            params,
        ) { rs, _ -> rs.getLong("total") }.firstOrNull() ?: 0

        val items = jdbc.query(
            """
            SELECT id, visitor_id, started_at, ended_at, duration_seconds, pageviews, entry_path,
                   exit_path, is_bounce, country, device_type, browser, os, referrer_domain
            FROM sessions
            WHERE $conditions
            ORDER BY started_at DESC
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            params.addValue("limit", size).addValue("offset", page * size),
        ) { rs, _ ->
            SessionRow(
                id = rs.getString("id"),
                visitorId = rs.getString("visitor_id"),
                startedAt = rs.getTimestamp("started_at").toInstant(),
                endedAt = rs.getTimestamp("ended_at")?.toInstant(),
                durationSeconds = rs.getInt("duration_seconds"),
                pageviews = rs.getInt("pageviews"),
                entryPath = rs.getString("entry_path"),
                exitPath = rs.getString("exit_path"),
                isBounce = rs.getBoolean("is_bounce"),
                country = rs.getString("country"),
                deviceType = rs.getString("device_type"),
                browser = rs.getString("browser"),
                os = rs.getString("os"),
                referrerDomain = rs.getString("referrer_domain"),
            )
        }
        return PageResponse(items, page, size, total)
    }

    private fun timeParams(projectId: java.util.UUID, from: Instant, to: Instant): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("from", Timestamp.from(from))
            .addValue("to", Timestamp.from(to))
}

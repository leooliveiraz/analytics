package com.analytics.analytics

import com.analytics.analytics.dto.BreakdownItem
import com.analytics.analytics.dto.ElementStatRow
import com.analytics.analytics.dto.EventRow
import com.analytics.analytics.dto.GeoStatItem
import com.analytics.analytics.dto.HeatmapPoint
import com.analytics.analytics.dto.ImageStatRow
import com.analytics.analytics.dto.PageMetricRow
import com.analytics.analytics.dto.PageResponse
import com.analytics.analytics.dto.RealtimePoint
import com.analytics.analytics.dto.SectionStatRow
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

    fun pages(projectId: java.util.UUID, from: Instant, to: Instant, limit: Int): List<PageMetricRow> {
        val sql = """
            SELECT path,
                   COUNT(*) FILTER (WHERE event_name = 'pageview') AS pageviews,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(DISTINCT session_id) FILTER (WHERE event_name = 'pageview') AS entries,
                   COALESCE(AVG(duration_ms) FILTER (WHERE event_name = 'pageleave' AND duration_ms IS NOT NULL), 0) AS avg_time_ms,
                   COALESCE(AVG(scroll_pct) FILTER (WHERE event_name = 'pageleave' AND scroll_pct IS NOT NULL), 0) AS avg_scroll_pct
            FROM events
            WHERE project_id = :projectId
              AND occurred_at >= :from AND occurred_at < :to
              AND path IS NOT NULL
              AND event_name IN ('pageview', 'pageleave')
            GROUP BY path
            ORDER BY pageviews DESC, visitors DESC
            LIMIT :limit
        """.trimIndent()
        return jdbc.query(sql, timeParams(projectId, from, to).addValue("limit", limit)) { rs, _ ->
            PageMetricRow(
                path = rs.getString("path"),
                pageviews = rs.getLong("pageviews"),
                visitors = rs.getLong("visitors"),
                entries = rs.getLong("entries"),
                avgTimeMs = rs.getDouble("avg_time_ms"),
                avgScrollPct = rs.getDouble("avg_scroll_pct"),
            )
        }
    }

    fun elements(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        path: String?,
        limit: Int,
    ): List<ElementStatRow> {
        val conditions = StringBuilder(
            "project_id = :projectId AND occurred_at >= :from AND occurred_at < :to " +
                "AND element_selector IS NOT NULL AND event_name <> 'mousemove'",
        )
        val params = timeParams(projectId, from, to)
        path?.let {
            conditions.append(" AND path = :path")
            params.addValue("path", it)
        }
        val sql = """
            SELECT element_selector AS selector,
                   event_name,
                   MAX(element_text) AS element_text,
                   MAX(element_tag) AS element_tag,
                   COUNT(*) AS clicks,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(DISTINCT session_id) AS sessions
            FROM events
            WHERE $conditions
            GROUP BY element_selector, event_name
            ORDER BY clicks DESC
            LIMIT :limit
        """.trimIndent()
        return jdbc.query(sql, params.addValue("limit", limit)) { rs, _ ->
            ElementStatRow(
                selector = rs.getString("selector"),
                eventName = rs.getString("event_name"),
                text = rs.getString("element_text"),
                tag = rs.getString("element_tag"),
                clicks = rs.getLong("clicks"),
                visitors = rs.getLong("visitors"),
                sessions = rs.getLong("sessions"),
            )
        }
    }

    fun images(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        path: String?,
        limit: Int,
    ): List<ImageStatRow> {
        val conditions = StringBuilder(
            "project_id = :projectId AND occurred_at >= :from AND occurred_at < :to " +
                "AND event_name = 'image_view' AND image_key IS NOT NULL",
        )
        val params = timeParams(projectId, from, to)
        path?.let {
            conditions.append(" AND path = :path")
            params.addValue("path", it)
        }
        val sql = """
            SELECT image_key,
                   MAX(image_alt) AS image_alt,
                   COUNT(*) AS impressions,
                   COUNT(DISTINCT session_id) AS sessions,
                   COALESCE(AVG(dwell_ms) FILTER (WHERE dwell_ms IS NOT NULL), 0) AS avg_dwell_ms
            FROM events
            WHERE $conditions
            GROUP BY image_key
            ORDER BY impressions DESC
            LIMIT :limit
        """.trimIndent()
        return jdbc.query(sql, params.addValue("limit", limit)) { rs, _ ->
            ImageStatRow(
                imageKey = rs.getString("image_key"),
                imageAlt = rs.getString("image_alt"),
                impressions = rs.getLong("impressions"),
                sessions = rs.getLong("sessions"),
                avgDwellMs = rs.getDouble("avg_dwell_ms"),
            )
        }
    }

    fun sections(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        path: String?,
        limit: Int,
    ): List<SectionStatRow> {
        val conditions = StringBuilder(
            "project_id = :projectId AND occurred_at >= :from AND occurred_at < :to " +
                "AND event_name = 'section_engage' AND section_key IS NOT NULL",
        )
        val params = timeParams(projectId, from, to)
        path?.let {
            conditions.append(" AND path = :path")
            params.addValue("path", it)
        }
        val sql = """
            SELECT section_key,
                   COUNT(*) AS views,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COALESCE(AVG(duration_ms) FILTER (WHERE duration_ms IS NOT NULL), 0) AS avg_dwell_ms
            FROM events
            WHERE $conditions
            GROUP BY section_key
            ORDER BY avg_dwell_ms DESC
            LIMIT :limit
        """.trimIndent()
        return jdbc.query(sql, params.addValue("limit", limit)) { rs, _ ->
            SectionStatRow(
                sectionKey = rs.getString("section_key"),
                views = rs.getLong("views"),
                visitors = rs.getLong("visitors"),
                avgDwellMs = rs.getDouble("avg_dwell_ms"),
            )
        }
    }

    fun heatmap(
        projectId: java.util.UUID,
        from: Instant,
        to: Instant,
        eventName: String,
        path: String?,
        deviceType: String?,
        scroll: Boolean,
        limit: Int,
    ): List<HeatmapPoint> {
        val params = timeParams(projectId, from, to)
        val conditions = StringBuilder("project_id = :projectId AND occurred_at >= :from AND occurred_at < :to")
        path?.let {
            conditions.append(" AND path = :path")
            params.addValue("path", it)
        }
        val sql = if (scroll) {
            """
            SELECT 50.0 AS x, ROUND(scroll_pct)::double precision AS y, COUNT(*) AS weight
            FROM events
            WHERE $conditions AND event_name = 'pageleave' AND scroll_pct IS NOT NULL
            GROUP BY y
            ORDER BY y ASC
            LIMIT :limit
            """.trimIndent()
        } else {
            conditions.append(" AND event_name = :eventName AND click_x_pct IS NOT NULL AND click_y_pct IS NOT NULL")
            deviceType?.let {
                conditions.append(" AND device_type = :deviceType")
                params.addValue("deviceType", it)
            }
            params.addValue("eventName", eventName)
            """
            SELECT ROUND(click_x_pct)::double precision AS x,
                   ROUND(click_y_pct)::double precision AS y,
                   COUNT(*) AS weight
            FROM events
            WHERE $conditions
            GROUP BY x, y
            ORDER BY weight DESC
            LIMIT :limit
            """.trimIndent()
        }
        return jdbc.query(sql, params.addValue("limit", limit)) { rs, _ ->
            HeatmapPoint(
                x = rs.getDouble("x"),
                y = rs.getDouble("y"),
                weight = rs.getLong("weight"),
            )
        }
    }

    fun geo(projectId: java.util.UUID, from: Instant, to: Instant): List<GeoStatItem> {
        val sql = """
            SELECT country,
                   COUNT(DISTINCT visitor_id) AS visitors,
                   COUNT(*) FILTER (WHERE event_name = 'pageview') AS pageviews
            FROM events
            WHERE project_id = :projectId
              AND occurred_at >= :from AND occurred_at < :to
              AND country IS NOT NULL
            GROUP BY country
            ORDER BY visitors DESC
        """.trimIndent()
        return jdbc.query(sql, timeParams(projectId, from, to)) { rs, _ ->
            GeoStatItem(
                country = rs.getString("country"),
                visitors = rs.getLong("visitors"),
                pageviews = rs.getLong("pageviews"),
            )
        }
    }

    fun findSession(projectId: java.util.UUID, sessionId: java.util.UUID): SessionRow? {
        val sql = """
            SELECT id, visitor_id, started_at, ended_at, duration_seconds, pageviews, entry_path,
                   exit_path, is_bounce, country, device_type, browser, os, referrer_domain
            FROM sessions
            WHERE project_id = :projectId AND id = :sessionId
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sessionId", sessionId)
        return jdbc.query(sql, params) { rs, _ -> mapSession(rs) }.firstOrNull()
    }

    fun eventsForSession(projectId: java.util.UUID, sessionId: java.util.UUID, limit: Int): List<EventRow> {
        val sql = """
            SELECT id, event_name, path, url, referrer_domain, country, device_type, browser, os,
                   visitor_id, session_id, occurred_at
            FROM events
            WHERE project_id = :projectId AND session_id = :sessionId
            ORDER BY occurred_at ASC
            LIMIT :limit
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("sessionId", sessionId)
            .addValue("limit", limit)
        return jdbc.query(sql, params) { rs, _ -> mapEvent(rs) }
    }

    private fun mapEvent(rs: java.sql.ResultSet): EventRow = EventRow(
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

    private fun mapSession(rs: java.sql.ResultSet): SessionRow = SessionRow(
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

    private fun timeParams(projectId: java.util.UUID, from: Instant, to: Instant): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("from", Timestamp.from(from))
            .addValue("to", Timestamp.from(to))
}

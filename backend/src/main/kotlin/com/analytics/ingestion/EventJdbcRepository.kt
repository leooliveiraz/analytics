package com.analytics.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.UUID

@Repository
class EventJdbcRepository(
    private val jdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {

    fun insertAll(projectId: UUID, events: List<EnrichedEvent>) {
        if (events.isEmpty()) return
        val params = events.map { toEventParams(projectId, it) }.toTypedArray()
        jdbc.batchUpdate(INSERT_EVENT, params)
    }

    fun upsertSessions(projectId: UUID, sessions: List<SessionUpsert>) {
        if (sessions.isEmpty()) return
        val params = sessions.map { toSessionParams(projectId, it) }.toTypedArray()
        jdbc.batchUpdate(UPSERT_SESSION, params)
    }

    private fun toEventParams(projectId: UUID, event: EnrichedEvent): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("eventName", event.eventName)
            .addValue("url", event.url)
            .addValue("path", event.path)
            .addValue("referrer", event.referrer)
            .addValue("referrerDomain", event.referrerDomain)
            .addValue("utmSource", event.utmSource)
            .addValue("utmMedium", event.utmMedium)
            .addValue("utmCampaign", event.utmCampaign)
            .addValue("utmTerm", event.utmTerm)
            .addValue("utmContent", event.utmContent)
            .addValue("browser", event.browser)
            .addValue("os", event.os)
            .addValue("deviceType", event.deviceType)
            .addValue("country", event.country)
            .addValue("region", event.region)
            .addValue("city", event.city)
            .addValue("language", event.language)
            .addValue("screenWidth", event.screenWidth)
            .addValue("screenHeight", event.screenHeight)
            .addValue("visitorId", event.visitorId)
            .addValue("sessionId", event.sessionId)
            .addValue("elementTag", event.elementTag)
            .addValue("elementSelector", event.elementSelector)
            .addValue("elementText", event.elementText)
            .addValue("elementId", event.elementId)
            .addValue("href", event.href)
            .addValue("clickX", event.clickX)
            .addValue("clickY", event.clickY)
            .addValue("clickXPct", event.clickXPct)
            .addValue("clickYPct", event.clickYPct)
            .addValue("viewportWidth", event.viewportWidth)
            .addValue("viewportHeight", event.viewportHeight)
            .addValue("pageHeight", event.pageHeight)
            .addValue("durationMs", event.durationMs)
            .addValue("engagedMs", event.engagedMs)
            .addValue("scrollPct", event.scrollPct)
            .addValue("imageKey", event.imageKey)
            .addValue("imageAlt", event.imageAlt)
            .addValue("dwellMs", event.dwellMs)
            .addValue("sectionKey", event.sectionKey)
            .addValue("properties", objectMapper.writeValueAsString(event.properties))
            .addValue("occurredAt", java.sql.Timestamp.from(event.occurredAt))

    private fun toSessionParams(projectId: UUID, session: SessionUpsert): MapSqlParameterSource =
        MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("id", session.id)
            .addValue("visitorId", session.visitorId)
            .addValue("startedAt", java.sql.Timestamp.from(session.startedAt))
            .addValue("endedAt", java.sql.Timestamp.from(session.endedAt))
            .addValue("durationSeconds", Duration.between(session.startedAt, session.endedAt).seconds.toInt())
            .addValue("pageviews", session.pageviews)
            .addValue("entryPath", session.entryPath)
            .addValue("exitPath", session.exitPath)
            .addValue("isBounce", session.isBounce)
            .addValue("referrerDomain", session.referrerDomain)
            .addValue("utmSource", session.utmSource)
            .addValue("utmMedium", session.utmMedium)
            .addValue("utmCampaign", session.utmCampaign)
            .addValue("country", session.country)
            .addValue("browser", session.browser)
            .addValue("os", session.os)
            .addValue("deviceType", session.deviceType)

    companion object {
        private val INSERT_EVENT = """
            INSERT INTO events (
                project_id, event_name, url, path, referrer, referrer_domain,
                utm_source, utm_medium, utm_campaign, utm_term, utm_content,
                browser, os, device_type, country, region, city, language,
                screen_width, screen_height, visitor_id, session_id,
                element_tag, element_selector, element_text, element_id, href,
                click_x, click_y, click_x_pct, click_y_pct,
                viewport_width, viewport_height, page_height,
                duration_ms, engaged_ms, scroll_pct,
                image_key, image_alt, dwell_ms, section_key,
                properties, occurred_at
            ) VALUES (
                :projectId, :eventName, :url, :path, :referrer, :referrerDomain,
                :utmSource, :utmMedium, :utmCampaign, :utmTerm, :utmContent,
                :browser, :os, :deviceType, :country, :region, :city, :language,
                :screenWidth, :screenHeight, :visitorId, :sessionId,
                :elementTag, :elementSelector, :elementText, :elementId, :href,
                :clickX, :clickY, :clickXPct, :clickYPct,
                :viewportWidth, :viewportHeight, :pageHeight,
                :durationMs, :engagedMs, :scrollPct,
                :imageKey, :imageAlt, :dwellMs, :sectionKey,
                CAST(:properties AS jsonb), :occurredAt
            )
        """.trimIndent()

        private val UPSERT_SESSION = """
            INSERT INTO sessions (
                id, project_id, visitor_id, started_at, ended_at, duration_seconds, pageviews,
                entry_path, exit_path, is_bounce, referrer_domain, utm_source, utm_medium, utm_campaign,
                country, browser, os, device_type, updated_at
            ) VALUES (
                :id, :projectId, :visitorId, :startedAt, :endedAt,
                :durationSeconds, :pageviews,
                :entryPath, :exitPath, :isBounce, :referrerDomain, :utmSource, :utmMedium, :utmCampaign,
                :country, :browser, :os, :deviceType, now()
            )
            ON CONFLICT (id) DO UPDATE SET
                started_at       = LEAST(sessions.started_at, EXCLUDED.started_at),
                ended_at         = GREATEST(sessions.ended_at, EXCLUDED.ended_at),
                duration_seconds = EXTRACT(EPOCH FROM (GREATEST(sessions.ended_at, EXCLUDED.ended_at)
                                     - LEAST(sessions.started_at, EXCLUDED.started_at)))::int,
                pageviews        = sessions.pageviews + EXCLUDED.pageviews,
                exit_path        = EXCLUDED.exit_path,
                is_bounce        = (sessions.pageviews + EXCLUDED.pageviews) <= 1,
                updated_at       = now()
        """.trimIndent()
    }
}

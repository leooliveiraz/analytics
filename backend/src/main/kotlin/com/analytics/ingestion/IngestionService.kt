package com.analytics.ingestion

import com.analytics.common.util.TokenGenerator
import com.analytics.config.IngestionProperties
import com.analytics.ingestion.dto.IngestEventDto
import com.analytics.ingestion.dto.IngestRequest
import com.analytics.ingestion.dto.IngestResponse
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class IngestionService(
    private val projectKeyAuthenticator: ProjectKeyAuthenticator,
    private val userAgentService: UserAgentService,
    private val geoIpService: GeoIpService,
    private val eventJdbcRepository: EventJdbcRepository,
    private val properties: IngestionProperties,
) {

    fun ingest(apiKey: String?, clientIp: String?, userAgent: String?, request: IngestRequest): IngestResponse {
        val projectId = projectKeyAuthenticator.resolveProjectId(apiKey)
        val client = userAgentService.parse(userAgent)
        val geo = geoIpService.lookup(clientIp)

        var dropped = 0
        val enriched = ArrayList<EnrichedEvent>(request.events.size)
        for (dto in request.events) {
            if (properties.filterBots && client.bot) {
                dropped++
                continue
            }
            enriched += enrich(projectId, dto, clientIp, userAgent, client, geo)
        }

        eventJdbcRepository.insertAll(projectId, enriched)
        eventJdbcRepository.upsertSessions(projectId, aggregateSessions(enriched))
        return IngestResponse(accepted = enriched.size, dropped = dropped)
    }

    private fun enrich(
        projectId: UUID,
        dto: IngestEventDto,
        clientIp: String?,
        userAgent: String?,
        client: ClientInfo,
        geo: GeoLocation,
    ): EnrichedEvent {
        val occurredAt = clamp(dto.occurredAt ?: Instant.now())
        val url = dto.url?.trim()?.takeIf { it.isNotEmpty() }
        val uri = parseUri(url)
        val query = parseQuery(uri)
        val referrer = dto.referrer?.trim()?.takeIf { it.isNotEmpty() }
        val visitorId = dto.visitorId?.trim()?.takeIf { it.isNotEmpty() }
            ?: deriveVisitorId(projectId, clientIp, userAgent, occurredAt)
        val sessionId = dto.sessionId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: UUID.randomUUID()

        return EnrichedEvent(
            eventName = dto.name.trim().ifEmpty { "pageview" }.take(120),
            url = url,
            path = uri?.path?.takeIf { it.isNotEmpty() }?.take(2048),
            referrer = referrer,
            referrerDomain = parseHost(referrer),
            utmSource = query["utm_source"]?.take(255),
            utmMedium = query["utm_medium"]?.take(255),
            utmCampaign = query["utm_campaign"]?.take(255),
            utmTerm = query["utm_term"]?.take(255),
            utmContent = query["utm_content"]?.take(255),
            browser = client.browser?.take(64),
            os = client.os?.take(64),
            deviceType = client.deviceType?.take(32),
            country = geo.country,
            region = geo.region?.take(120),
            city = geo.city?.take(120),
            language = dto.language?.take(16),
            screenWidth = dto.screenWidth,
            screenHeight = dto.screenHeight,
            visitorId = visitorId.take(64),
            sessionId = sessionId,
            properties = dto.properties,
            occurredAt = occurredAt,
        )
    }

    private fun aggregateSessions(events: List<EnrichedEvent>): List<SessionUpsert> =
        events.groupBy { it.sessionId }.map { (sessionId, group) ->
            val sorted = group.sortedBy { it.occurredAt }
            val first = sorted.first()
            val last = sorted.last()
            val pageviews = sorted.count { it.eventName == "pageview" }
            SessionUpsert(
                id = sessionId,
                visitorId = first.visitorId,
                startedAt = first.occurredAt,
                endedAt = last.occurredAt,
                pageviews = pageviews,
                entryPath = first.path,
                exitPath = last.path,
                isBounce = pageviews <= 1,
                referrerDomain = first.referrerDomain,
                utmSource = first.utmSource,
                utmMedium = first.utmMedium,
                utmCampaign = first.utmCampaign,
                country = first.country,
                browser = first.browser,
                os = first.os,
                deviceType = first.deviceType,
            )
        }

    private fun deriveVisitorId(projectId: UUID, clientIp: String?, userAgent: String?, occurredAt: Instant): String {
        val day = occurredAt.atZone(ZoneOffset.UTC).toLocalDate().toString()
        val raw = "${clientIp.orEmpty()}|${userAgent.orEmpty()}|${properties.visitorSalt}|$projectId|$day"
        return TokenGenerator.sha256Hex(raw)
    }

    private fun clamp(instant: Instant): Instant {
        val now = Instant.now()
        return if (instant.isAfter(now.plus(5, ChronoUnit.MINUTES))) now else instant
    }

    private fun parseUri(url: String?): URI? =
        if (url.isNullOrBlank()) null else runCatching { URI(url) }.getOrNull()

    private fun parseHost(url: String?): String? =
        if (url.isNullOrBlank()) null else runCatching { URI(url).host }.getOrNull()?.take(255)

    private fun parseQuery(uri: URI?): Map<String, String> {
        val raw = uri?.rawQuery ?: return emptyMap()
        return raw.split("&").mapNotNull { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) {
                null
            } else {
                val key = URLDecoder.decode(pair.substring(0, index), StandardCharsets.UTF_8)
                val value = URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8)
                key to value
            }
        }.toMap()
    }
}

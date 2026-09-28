package com.analytics.ingestion

import com.analytics.common.util.TokenGenerator
import com.analytics.config.IngestionProperties
import com.analytics.ingestion.dto.IngestEventDto
import com.analytics.ingestion.dto.IngestRequest
import com.analytics.ingestion.dto.IngestResponse
import org.springframework.stereotype.Service
import java.net.InetAddress
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
        // With anonymizeIp, the last octet (IPv4) / last 80 bits (IPv6) are zeroed
        // before geolocation and visitor hashing, so the exact IP is never used.
        val effectiveIp = if (properties.anonymizeIp) anonymizeIp(clientIp) else clientIp
        val geo = geoIpService.lookup(effectiveIp)

        var dropped = 0
        val enriched = ArrayList<EnrichedEvent>(request.events.size)
        for (dto in request.events) {
            if (properties.filterBots && client.bot) {
                dropped++
                continue
            }
            enriched += enrich(projectId, dto, effectiveIp, userAgent, client, geo)
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
        val props = dto.properties
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
            elementTag = (dto.elementTag ?: props.str("elementTag"))?.trim()?.takeIf { it.isNotEmpty() }?.take(32),
            elementSelector = (dto.elementSelector ?: props.str("elementSelector"))?.trim()?.takeIf { it.isNotEmpty() }?.take(1024),
            elementText = (dto.elementText ?: props.str("elementText"))?.trim()?.takeIf { it.isNotEmpty() }?.take(512),
            elementId = (dto.elementId ?: props.str("elementId"))?.trim()?.takeIf { it.isNotEmpty() }?.take(255),
            href = (dto.href ?: props.str("href"))?.trim()?.takeIf { it.isNotEmpty() }?.take(4096),
            clickX = dto.clickX ?: props.int("clickX"),
            clickY = dto.clickY ?: props.int("clickY"),
            clickXPct = (dto.clickXPct ?: props.dbl("clickXPct"))?.coerceIn(0.0, 100.0),
            clickYPct = (dto.clickYPct ?: props.dbl("clickYPct"))?.coerceIn(0.0, 1000.0),
            viewportWidth = dto.viewportWidth ?: props.int("viewportWidth"),
            viewportHeight = dto.viewportHeight ?: props.int("viewportHeight"),
            pageHeight = dto.pageHeight ?: props.int("pageHeight"),
            durationMs = (dto.durationMs ?: props.int("durationMs"))?.coerceAtLeast(0),
            engagedMs = (dto.engagedMs ?: props.int("engagedMs"))?.coerceAtLeast(0),
            scrollPct = (dto.scrollPct ?: props.int("scrollPct"))?.coerceIn(0, 100),
            imageKey = (dto.imageKey ?: props.str("imageKey"))?.trim()?.takeIf { it.isNotEmpty() }?.take(1024),
            imageAlt = (dto.imageAlt ?: props.str("imageAlt"))?.trim()?.takeIf { it.isNotEmpty() }?.take(512),
            dwellMs = (dto.dwellMs ?: props.int("dwellMs"))?.coerceAtLeast(0),
            sectionKey = (dto.sectionKey ?: props.str("sectionKey"))?.trim()?.takeIf { it.isNotEmpty() }?.take(255),
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

    private fun Map<String, Any?>.str(key: String): String? = this[key] as? String

    private fun Map<String, Any?>.int(key: String): Int? = (this[key] as? Number)?.toInt()

    private fun Map<String, Any?>.dbl(key: String): Double? = (this[key] as? Number)?.toDouble()

    private fun anonymizeIp(ip: String?): String? {
        if (ip.isNullOrBlank()) {
            return ip
        }
        val address = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return ip
        val bytes = address.address
        when (bytes.size) {
            4 -> bytes[3] = 0
            16 -> for (i in 6 until 16) bytes[i] = 0
            else -> return ip
        }
        return runCatching { InetAddress.getByAddress(bytes).hostAddress }.getOrNull() ?: ip
    }
}

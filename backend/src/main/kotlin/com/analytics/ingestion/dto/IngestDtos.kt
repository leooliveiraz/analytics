package com.analytics.ingestion.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

data class IngestEventDto(
    @field:Size(max = 120) val name: String = "pageview",
    @field:Size(max = 4096) val url: String? = null,
    @field:Size(max = 4096) val referrer: String? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    @field:Size(max = 16) val language: String? = null,
    @field:Size(max = 128) val visitorId: String? = null,
    val sessionId: String? = null,
    val occurredAt: Instant? = null,
    // Element / click context
    @field:Size(max = 32) val elementTag: String? = null,
    @field:Size(max = 1024) val elementSelector: String? = null,
    @field:Size(max = 512) val elementText: String? = null,
    @field:Size(max = 255) val elementId: String? = null,
    @field:Size(max = 4096) val href: String? = null,
    val clickX: Int? = null,
    val clickY: Int? = null,
    val clickXPct: Double? = null,
    val clickYPct: Double? = null,
    val viewportWidth: Int? = null,
    val viewportHeight: Int? = null,
    val pageHeight: Int? = null,
    // Time / scroll
    val durationMs: Int? = null,
    val engagedMs: Int? = null,
    val scrollPct: Int? = null,
    // Media (image views)
    @field:Size(max = 1024) val imageKey: String? = null,
    @field:Size(max = 512) val imageAlt: String? = null,
    val dwellMs: Int? = null,
    // Sections
    @field:Size(max = 255) val sectionKey: String? = null,
    val properties: Map<String, Any?> = emptyMap(),
)

data class IngestRequest(
    @field:NotEmpty(message = "events must not be empty")
    @field:Size(max = 100, message = "at most 100 events per request")
    @field:Valid
    val events: List<IngestEventDto>,
)

data class IngestResponse(
    val accepted: Int,
    val dropped: Int,
)

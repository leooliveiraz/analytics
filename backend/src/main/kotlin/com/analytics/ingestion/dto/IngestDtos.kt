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

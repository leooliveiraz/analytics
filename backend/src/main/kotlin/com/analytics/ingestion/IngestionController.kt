package com.analytics.ingestion

import com.analytics.common.exception.TooManyRequestsException
import com.analytics.ingestion.dto.IngestRequest
import com.analytics.ingestion.dto.IngestResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class IngestionController(
    private val ingestionService: IngestionService,
    private val rateLimitService: RateLimitService,
) {

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun ingest(
        @RequestHeader(value = "X-Api-Key", required = false) apiKeyHeader: String?,
        @RequestParam(value = "k", required = false) apiKeyParam: String?,
        @Valid @RequestBody request: IngestRequest,
        httpRequest: HttpServletRequest,
    ): IngestResponse {
        val apiKey = apiKeyHeader ?: apiKeyParam ?: httpRequest.getHeader("X-Project-Key")
        val clientIp = resolveClientIp(httpRequest)
        val rateKey = apiKey ?: clientIp ?: "anonymous"
        if (!rateLimitService.tryConsume(rateKey)) {
            throw TooManyRequestsException()
        }
        return ingestionService.ingest(apiKey, clientIp, httpRequest.getHeader("User-Agent"), request)
    }

    private fun resolveClientIp(request: HttpServletRequest): String? {
        val forwarded = request.getHeader("X-Forwarded-For")
        if (!forwarded.isNullOrBlank()) {
            return forwarded.split(",").first().trim().takeIf { it.isNotEmpty() }
        }
        return request.getHeader("X-Real-IP")?.takeIf { it.isNotBlank() } ?: request.remoteAddr
    }
}

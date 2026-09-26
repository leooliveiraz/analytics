package com.analytics.apikey.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateApiKeyRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
)

data class ApiKeyResponse(
    val id: UUID,
    val name: String,
    val keyPrefix: String,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
)

data class ApiKeyCreatedResponse(
    val id: UUID,
    val name: String,
    val keyPrefix: String,
    val apiKey: String,
    val createdAt: Instant,
)

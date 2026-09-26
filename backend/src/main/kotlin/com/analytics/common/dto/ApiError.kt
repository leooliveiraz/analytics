package com.analytics.common.dto

import java.time.Instant

data class ApiError(
    val error: String,
    val details: Map<String, String>? = null,
    val timestamp: Instant = Instant.now(),
)

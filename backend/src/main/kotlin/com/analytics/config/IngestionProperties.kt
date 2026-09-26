package com.analytics.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.ingestion")
data class IngestionProperties(
    val visitorSalt: String,
    val filterBots: Boolean = true,
    val rateLimit: RateLimit = RateLimit(),
) {
    data class RateLimit(
        val enabled: Boolean = true,
        val capacity: Long = 240,
        val window: Duration = Duration.ofMinutes(1),
    )
}

@ConfigurationProperties(prefix = "app.geoip")
data class GeoIpProperties(
    val databasePath: String = "",
)

@ConfigurationProperties(prefix = "app.retention")
data class RetentionProperties(
    val eventsMonths: Int = 12,
)

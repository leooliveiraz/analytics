package com.analytics.ingestion

import com.analytics.config.IngestionProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitService(private val properties: IngestionProperties) {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(key: String): Boolean {
        if (!properties.rateLimit.enabled) return true
        val bucket = buckets.computeIfAbsent(key) { newBucket() }
        return bucket.tryConsume(1)
    }

    private fun newBucket(): Bucket {
        val capacity = properties.rateLimit.capacity
        val bandwidth = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, properties.rateLimit.window)
            .build()
        return Bucket.builder().addLimit(bandwidth).build()
    }
}

package com.analytics.ingestion

import com.analytics.config.RetentionProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.ZoneOffset

@Component
class PartitionManager(
    private val jdbc: JdbcTemplate,
    private val retentionProperties: RetentionProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun initialize() {
        runCatching {
            ensureFuturePartitions()
            applyRetention()
        }.onFailure { log.warn("Partition maintenance failed on startup: {}", it.message) }
    }

    @Scheduled(cron = "0 0 3 * * *")
    fun dailyMaintenance() {
        ensureFuturePartitions()
        applyRetention()
    }

    fun ensureFuturePartitions() {
        val current = YearMonth.now(ZoneOffset.UTC)
        for (offset in 0..MONTHS_AHEAD) {
            createPartitionIfMissing(current.plusMonths(offset.toLong()))
        }
    }

    fun applyRetention() {
        val months = retentionProperties.eventsMonths
        if (months <= 0) return
        val cutoff = YearMonth.now(ZoneOffset.UTC).minusMonths(months.toLong())
        existingEventPartitions().forEach { name ->
            val partitionMonth = parsePartitionMonth(name) ?: return@forEach
            if (partitionMonth.isBefore(cutoff)) {
                log.info("Dropping expired events partition {}", name)
                jdbc.execute("DROP TABLE IF EXISTS $name")
            }
        }
    }

    private fun createPartitionIfMissing(month: YearMonth) {
        val name = partitionName(month)
        val from = month.atDay(1)
        val to = month.plusMonths(1).atDay(1)
        jdbc.execute(
            "CREATE TABLE IF NOT EXISTS $name PARTITION OF events FOR VALUES FROM ('$from') TO ('$to')",
        )
    }

    private fun existingEventPartitions(): List<String> =
        jdbc.queryForList(
            """
            SELECT c.relname AS name
            FROM pg_inherits i
            JOIN pg_class c ON c.oid = i.inhrelid
            JOIN pg_class p ON p.oid = i.inhparent
            WHERE p.relname = 'events'
            """.trimIndent(),
            String::class.java,
        )

    private fun parsePartitionMonth(name: String): YearMonth? {
        val match = PARTITION_PATTERN.matchEntire(name) ?: return null
        return runCatching { YearMonth.of(match.groupValues[1].toInt(), match.groupValues[2].toInt()) }.getOrNull()
    }

    private fun partitionName(month: YearMonth): String = "events_%04d_%02d".format(month.year, month.monthValue)

    companion object {
        private const val MONTHS_AHEAD = 2
        private val PARTITION_PATTERN = Regex("""events_(\d{4})_(\d{2})""")
    }
}

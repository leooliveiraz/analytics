package com.analytics.ingestion

import com.analytics.config.GeoIpProperties
import com.maxmind.geoip2.DatabaseReader
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress
import java.time.Instant

data class GeoLocation(
    val country: String?,
    val region: String?,
    val city: String?,
)

@Service
class GeoIpService(private val properties: GeoIpProperties) {

    private val log = LoggerFactory.getLogger(javaClass)
    private var reader: DatabaseReader? = null
    private var lastModified: Long = 0

    @PostConstruct
    fun init() {
        load(force = true)
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    fun reloadIfChanged() {
        load(force = false)
    }

    @PreDestroy
    fun close() {
        reader?.close()
    }

    private fun load(force: Boolean) {
        val path = properties.databasePath
        if (path.isBlank()) {
            if (force) log.info("GeoIP disabled (app.geoip.database-path not set)")
            return
        }
        val file = File(path)
        if (!file.exists()) {
            if (force) log.warn("GeoIP database not found at {}, disabling", path)
            return
        }
        val modified = file.lastModified()
        if (!force && reader != null && modified <= lastModified) {
            return
        }
        runCatching {
            val newReader = DatabaseReader.Builder(file).build()
            reader?.close()
            reader = newReader
            lastModified = modified
            log.info("GeoIP database loaded from {} (modified {})", path, Instant.ofEpochMilli(modified))
        }.onFailure { log.warn("Failed to load GeoIP database from {}: {}", path, it.message) }
    }

    fun lookup(ip: String?): GeoLocation {
        val activeReader = reader ?: return GeoLocation(null, null, null)
        if (ip.isNullOrBlank()) return GeoLocation(null, null, null)
        val address = try {
            InetAddress.getByName(ip)
        } catch (_: Exception) {
            return GeoLocation(null, null, null)
        }
        if (address.isLoopbackAddress || address.isSiteLocalAddress) {
            return GeoLocation(null, null, null)
        }
        return try {
            val city = runCatching { activeReader.tryCity(address).orElse(null) }.getOrNull()
            if (city != null) {
                GeoLocation(
                    country = city.country?.isoCode,
                    region = city.mostSpecificSubdivision?.name,
                    city = city.city?.name,
                )
            } else {
                lookupCountry(activeReader, address)
            }
        } catch (_: Exception) {
            lookupCountry(activeReader, address)
        }
    }

    private fun lookupCountry(reader: DatabaseReader, address: InetAddress): GeoLocation {
        val country = runCatching { reader.tryCountry(address).orElse(null) }.getOrNull()
        return GeoLocation(country = country?.country?.isoCode, region = null, city = null)
    }
}

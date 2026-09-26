package com.analytics.ingestion

import com.analytics.config.GeoIpProperties
import com.maxmind.geoip2.DatabaseReader
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress

data class GeoLocation(
    val country: String?,
    val region: String?,
    val city: String?,
)

@Service
class GeoIpService(private val properties: GeoIpProperties) {

    private val log = LoggerFactory.getLogger(javaClass)
    private var reader: DatabaseReader? = null

    @PostConstruct
    fun init() {
        val path = properties.databasePath
        if (path.isBlank()) {
            log.info("GeoIP disabled (app.geoip.database-path not set)")
            return
        }
        val file = File(path)
        if (!file.exists()) {
            log.warn("GeoIP database not found at {}, disabling", path)
            return
        }
        reader = DatabaseReader.Builder(file).build()
        log.info("GeoIP database loaded from {}", path)
    }

    @PreDestroy
    fun close() {
        reader?.close()
    }

    fun lookup(ip: String?): GeoLocation {
        val activeReader = reader ?: return GeoLocation(null, null, null)
        if (ip.isNullOrBlank()) return GeoLocation(null, null, null)
        return try {
            val address = InetAddress.getByName(ip)
            if (address.isLoopbackAddress || address.isSiteLocalAddress) {
                return GeoLocation(null, null, null)
            }
            val response = activeReader.tryCity(address).orElse(null) ?: return GeoLocation(null, null, null)
            GeoLocation(
                country = response.country?.isoCode,
                region = response.mostSpecificSubdivision?.name,
                city = response.city?.name,
            )
        } catch (_: Exception) {
            GeoLocation(null, null, null)
        }
    }
}

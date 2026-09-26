package com.analytics.ingestion

import org.springframework.stereotype.Service
import ua_parser.Parser

data class ClientInfo(
    val browser: String?,
    val os: String?,
    val deviceType: String?,
    val bot: Boolean,
)

@Service
class UserAgentService {

    private val parser = Parser()

    fun parse(userAgent: String?): ClientInfo {
        if (userAgent.isNullOrBlank()) {
            return ClientInfo(null, null, null, false)
        }
        val detectedBot = BOT_PATTERN.containsMatchIn(userAgent)
        return try {
            val client = parser.parse(userAgent)
            val deviceFamily = client.device?.family
            val osFamily = client.os?.family
            val browserFamily = client.userAgent?.family
            ClientInfo(
                browser = browserFamily?.takeIf { it.isNotBlank() },
                os = osFamily?.takeIf { it.isNotBlank() },
                deviceType = resolveDeviceType(deviceFamily, osFamily, detectedBot),
                bot = detectedBot || deviceFamily.equals("Spider", ignoreCase = true),
            )
        } catch (_: Exception) {
            ClientInfo(null, null, null, detectedBot)
        }
    }

    private fun resolveDeviceType(deviceFamily: String?, osFamily: String?, bot: Boolean): String? {
        if (bot) return "bot"
        val haystack = "${deviceFamily.orEmpty()} ${osFamily.orEmpty()}".lowercase()
        return when {
            "tablet" in haystack || "ipad" in haystack -> "tablet"
            "mobile" in haystack || "phone" in haystack || "android" in haystack || "ios" in haystack -> "mobile"
            "spider" in haystack || "bot" in haystack || "crawler" in haystack -> "bot"
            else -> "desktop"
        }
    }

    companion object {
        private val BOT_PATTERN = Regex(
            "(?i)(bot|crawler|spider|slurp|bingpreview|facebookexternalhit|headlesschrome|" +
                "python-requests|curl/|wget/|monitoring|uptimerobot|pingdom|lighthouse)",
        )
    }
}

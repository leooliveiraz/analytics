package com.analytics.common.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object TokenGenerator {

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun randomToken(lengthBytes: Int = 24): String {
        val bytes = ByteArray(lengthBytes)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

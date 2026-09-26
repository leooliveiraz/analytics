package com.analytics.security

import com.analytics.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(private val properties: JwtProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(properties.secret.toByteArray(Charsets.UTF_8))

    fun generateAccessToken(userId: UUID, email: String): String =
        buildToken(userId, email, TYPE_ACCESS, properties.accessTokenTtl)

    fun generateRefreshToken(userId: UUID, email: String): String =
        buildToken(userId, email, TYPE_REFRESH, properties.refreshTokenTtl)

    fun parse(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer)
            .build()
            .parseSignedClaims(token)
            .payload

    fun accessTokenTtlSeconds(): Long = properties.accessTokenTtl.seconds

    fun refreshTokenTtl(): Duration = properties.refreshTokenTtl

    private fun buildToken(userId: UUID, email: String, type: String, ttl: Duration): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .issuer(properties.issuer)
            .claim(CLAIM_EMAIL, email)
            .claim(CLAIM_TYPE, type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key)
            .compact()
    }

    companion object {
        const val TYPE_ACCESS = "access"
        const val TYPE_REFRESH = "refresh"
        const val CLAIM_TYPE = "type"
        const val CLAIM_EMAIL = "email"
    }
}

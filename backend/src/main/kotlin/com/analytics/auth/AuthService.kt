package com.analytics.auth

import com.analytics.auth.dto.LoginRequest
import com.analytics.auth.dto.RefreshRequest
import com.analytics.auth.dto.RegisterRequest
import com.analytics.auth.dto.TokenResponse
import com.analytics.common.exception.ConflictException
import com.analytics.common.exception.UnauthorizedException
import com.analytics.common.util.TokenGenerator
import com.analytics.security.JwtService
import com.analytics.user.User
import com.analytics.user.UserRepository
import io.jsonwebtoken.JwtException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(request: RegisterRequest): TokenResponse {
        val email = normalize(request.email)
        if (userRepository.existsByEmail(email)) {
            throw ConflictException("Email already registered")
        }
        val user = User().apply {
            this.email = email
            this.passwordHash = passwordEncoder.encode(request.password)
            this.name = request.name?.trim()
        }
        return issueTokens(userRepository.save(user))
    }

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(normalize(request.email))
            ?: throw UnauthorizedException("Invalid credentials")
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }
        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshRequest): TokenResponse {
        val claims = try {
            jwtService.parse(request.refreshToken)
        } catch (_: JwtException) {
            throw UnauthorizedException("Invalid refresh token")
        }
        if (claims[JwtService.CLAIM_TYPE] != JwtService.TYPE_REFRESH) {
            throw UnauthorizedException("Invalid refresh token")
        }

        val stored = refreshTokenRepository.findByTokenHash(TokenGenerator.sha256Hex(request.refreshToken))
            ?: throw UnauthorizedException("Refresh token not recognized")
        if (stored.revokedAt != null || stored.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException("Refresh token expired or revoked")
        }
        stored.revokedAt = Instant.now()
        refreshTokenRepository.save(stored)

        val user = userRepository.findById(UUID.fromString(claims.subject))
            .orElseThrow { UnauthorizedException("User no longer exists") }
        return issueTokens(user)
    }

    @Transactional
    fun logout(userId: UUID) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now())
    }

    private fun issueTokens(user: User): TokenResponse {
        val userId = requireNotNull(user.id) { "User id must not be null" }
        val accessToken = jwtService.generateAccessToken(userId, user.email)
        val refreshToken = jwtService.generateRefreshToken(userId, user.email)
        refreshTokenRepository.save(
            RefreshToken().apply {
                this.userId = userId
                this.tokenHash = TokenGenerator.sha256Hex(refreshToken)
                this.expiresAt = Instant.now().plus(jwtService.refreshTokenTtl())
            },
        )
        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.accessTokenTtlSeconds(),
        )
    }

    private fun normalize(email: String): String = email.trim().lowercase()
}

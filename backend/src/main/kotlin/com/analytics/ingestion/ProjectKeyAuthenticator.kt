package com.analytics.ingestion

import com.analytics.apikey.ApiKeyRepository
import com.analytics.common.exception.UnauthorizedException
import com.analytics.common.util.TokenGenerator
import com.analytics.project.ProjectRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class ProjectKeyAuthenticator(
    private val projectRepository: ProjectRepository,
    private val apiKeyRepository: ApiKeyRepository,
) {

    @Transactional
    fun resolveProjectId(rawKey: String?): UUID {
        val key = rawKey?.trim()
        if (key.isNullOrEmpty()) throw UnauthorizedException("Missing API key")

        return when {
            key.startsWith(PUBLIC_PREFIX) -> resolveByPublicKey(key)
            key.startsWith(SECRET_PREFIX) -> resolveBySecretKey(key)
            else -> throw UnauthorizedException("Invalid API key")
        }
    }

    private fun resolveByPublicKey(key: String): UUID {
        val project = projectRepository.findByPublicKey(key)
            ?: throw UnauthorizedException("Invalid API key")
        return requireNotNull(project.id)
    }

    private fun resolveBySecretKey(key: String): UUID {
        val candidates = apiKeyRepository.findByKeyPrefix(key.take(PREFIX_LENGTH))
        val hash = TokenGenerator.sha256Hex(key)
        val match = candidates.firstOrNull {
            MessageDigest.isEqual(it.keyHash.toByteArray(), hash.toByteArray())
        } ?: throw UnauthorizedException("Invalid API key")
        match.lastUsedAt = Instant.now()
        apiKeyRepository.save(match)
        return match.projectId
    }

    companion object {
        private const val PUBLIC_PREFIX = "pk_"
        private const val SECRET_PREFIX = "ak_"
        private const val PREFIX_LENGTH = 11
    }
}

package com.analytics.apikey

import com.analytics.apikey.dto.ApiKeyCreatedResponse
import com.analytics.apikey.dto.ApiKeyResponse
import com.analytics.apikey.dto.CreateApiKeyRequest
import com.analytics.common.exception.NotFoundException
import com.analytics.common.util.TokenGenerator
import com.analytics.project.ProjectService
import com.analytics.project.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val projectService: ProjectService,
) {

    @Transactional
    fun create(projectId: UUID, userId: UUID, request: CreateApiKeyRequest): ApiKeyCreatedResponse {
        projectService.requireRole(projectId, userId, Role.ADMIN)
        val secret = "ak_${TokenGenerator.randomToken(32)}"
        val entity = apiKeyRepository.save(
            ApiKey().apply {
                this.projectId = projectId
                this.name = request.name.trim()
                this.keyPrefix = secret.substring(0, PREFIX_LENGTH)
                this.keyHash = TokenGenerator.sha256Hex(secret)
            },
        )
        return ApiKeyCreatedResponse(
            id = requireNotNull(entity.id),
            name = entity.name,
            keyPrefix = entity.keyPrefix,
            apiKey = secret,
            createdAt = entity.createdAt,
        )
    }

    @Transactional(readOnly = true)
    fun list(projectId: UUID, userId: UUID): List<ApiKeyResponse> {
        projectService.requireRole(projectId, userId, Role.VIEWER)
        return apiKeyRepository.findAllByProjectId(projectId)
            .map { it.toResponse() }
            .sortedByDescending { it.createdAt }
    }

    @Transactional
    fun delete(projectId: UUID, userId: UUID, keyId: UUID) {
        projectService.requireRole(projectId, userId, Role.ADMIN)
        val key = apiKeyRepository.findByIdAndProjectId(keyId, projectId)
            ?: throw NotFoundException("API key not found")
        apiKeyRepository.delete(key)
    }

    private fun ApiKey.toResponse() = ApiKeyResponse(
        id = requireNotNull(id),
        name = name,
        keyPrefix = keyPrefix,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )

    companion object {
        private const val PREFIX_LENGTH = 11
    }
}

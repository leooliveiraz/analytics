package com.analytics.apikey

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApiKeyRepository : JpaRepository<ApiKey, UUID> {

    fun findAllByProjectId(projectId: UUID): List<ApiKey>

    fun findByIdAndProjectId(id: UUID, projectId: UUID): ApiKey?

    fun findByKeyPrefix(keyPrefix: String): List<ApiKey>
}

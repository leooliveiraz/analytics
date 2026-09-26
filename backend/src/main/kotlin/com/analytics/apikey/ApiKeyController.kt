package com.analytics.apikey

import com.analytics.apikey.dto.ApiKeyCreatedResponse
import com.analytics.apikey.dto.ApiKeyResponse
import com.analytics.apikey.dto.CreateApiKeyRequest
import com.analytics.security.SecurityUtils
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/api-keys")
class ApiKeyController(private val apiKeyService: ApiKeyService) {

    @GetMapping
    fun list(@PathVariable projectId: UUID): List<ApiKeyResponse> =
        apiKeyService.list(projectId, SecurityUtils.currentUserId())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: CreateApiKeyRequest,
    ): ApiKeyCreatedResponse = apiKeyService.create(projectId, SecurityUtils.currentUserId(), request)

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable projectId: UUID, @PathVariable keyId: UUID) {
        apiKeyService.delete(projectId, SecurityUtils.currentUserId(), keyId)
    }
}

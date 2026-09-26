package com.analytics.project.dto

import com.analytics.project.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank @field:Size(max = 150) val name: String,
    @field:Size(max = 255) val domain: String? = null,
    @field:Size(max = 64) val timezone: String? = null,
)

data class UpdateProjectRequest(
    @field:Size(max = 150) val name: String? = null,
    @field:Size(max = 255) val domain: String? = null,
    @field:Size(max = 64) val timezone: String? = null,
)

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val domain: String?,
    val timezone: String,
    val publicKey: String,
    val role: Role,
    val createdAt: Instant,
)

data class MemberResponse(
    val userId: UUID,
    val email: String,
    val name: String?,
    val role: Role,
    val createdAt: Instant,
)

data class AddMemberRequest(
    @field:NotBlank @field:Email val email: String,
    val role: Role = Role.VIEWER,
)

data class UpdateMemberRoleRequest(
    val role: Role,
)

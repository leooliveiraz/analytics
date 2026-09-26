package com.analytics.project

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectMemberRepository : JpaRepository<ProjectMember, UUID> {

    fun findAllByUserId(userId: UUID): List<ProjectMember>

    fun findAllByProjectId(projectId: UUID): List<ProjectMember>

    fun findByProjectIdAndUserId(projectId: UUID, userId: UUID): ProjectMember?

    fun existsByProjectIdAndUserId(projectId: UUID, userId: UUID): Boolean

    fun findAllByProjectIdAndRole(projectId: UUID, role: Role): List<ProjectMember>
}

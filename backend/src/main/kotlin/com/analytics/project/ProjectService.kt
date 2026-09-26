package com.analytics.project

import com.analytics.common.exception.ConflictException
import com.analytics.common.exception.ForbiddenException
import com.analytics.common.exception.NotFoundException
import com.analytics.common.util.TokenGenerator
import com.analytics.project.dto.AddMemberRequest
import com.analytics.project.dto.CreateProjectRequest
import com.analytics.project.dto.MemberResponse
import com.analytics.project.dto.ProjectResponse
import com.analytics.project.dto.UpdateMemberRoleRequest
import com.analytics.project.dto.UpdateProjectRequest
import com.analytics.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectMemberRepository: ProjectMemberRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun create(userId: UUID, request: CreateProjectRequest): ProjectResponse {
        val project = projectRepository.save(
            Project().apply {
                name = request.name.trim()
                domain = request.domain?.trim()
                timezone = request.timezone?.trim()?.takeIf { it.isNotEmpty() } ?: "UTC"
                publicKey = "pk_${TokenGenerator.randomToken(24)}"
            },
        )
        projectMemberRepository.save(
            ProjectMember().apply {
                this.projectId = requireNotNull(project.id)
                this.userId = userId
                this.role = Role.OWNER
            },
        )
        return project.toResponse(Role.OWNER)
    }

    @Transactional(readOnly = true)
    fun listForUser(userId: UUID): List<ProjectResponse> {
        val memberships = projectMemberRepository.findAllByUserId(userId)
        if (memberships.isEmpty()) return emptyList()
        val rolesByProject = memberships.associate { it.projectId to it.role }
        return projectRepository.findAllById(rolesByProject.keys)
            .map { it.toResponse(rolesByProject.getValue(requireNotNull(it.id))) }
            .sortedBy { it.name.lowercase() }
    }

    @Transactional(readOnly = true)
    fun getForUser(projectId: UUID, userId: UUID): ProjectResponse {
        val member = requireRole(projectId, userId, Role.VIEWER)
        return findProject(projectId).toResponse(member.role)
    }

    @Transactional
    fun update(projectId: UUID, userId: UUID, request: UpdateProjectRequest): ProjectResponse {
        val member = requireRole(projectId, userId, Role.ADMIN)
        val project = findProject(projectId)
        request.name?.let { project.name = it.trim() }
        request.domain?.let { project.domain = it.trim() }
        request.timezone?.let { project.timezone = it.trim() }
        return projectRepository.save(project).toResponse(member.role)
    }

    @Transactional
    fun delete(projectId: UUID, userId: UUID) {
        requireRole(projectId, userId, Role.OWNER)
        projectRepository.deleteById(projectId)
    }

    @Transactional(readOnly = true)
    fun listMembers(projectId: UUID, userId: UUID): List<MemberResponse> {
        requireRole(projectId, userId, Role.VIEWER)
        val members = projectMemberRepository.findAllByProjectId(projectId)
        val users = userRepository.findAllById(members.map { it.userId }).associateBy { it.id }
        return members
            .mapNotNull { member ->
                val user = users[member.userId] ?: return@mapNotNull null
                MemberResponse(requireNotNull(user.id), user.email, user.name, member.role, member.createdAt)
            }
            .sortedBy { it.email }
    }

    @Transactional
    fun addMember(projectId: UUID, userId: UUID, request: AddMemberRequest): MemberResponse {
        requireRole(projectId, userId, Role.ADMIN)
        val target = userRepository.findByEmail(request.email.trim().lowercase())
            ?: throw NotFoundException("User with this email not found")
        val targetId = requireNotNull(target.id)
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, targetId)) {
            throw ConflictException("User is already a member of this project")
        }
        val member = projectMemberRepository.save(
            ProjectMember().apply {
                this.projectId = projectId
                this.userId = targetId
                this.role = request.role
            },
        )
        return MemberResponse(targetId, target.email, target.name, member.role, member.createdAt)
    }

    @Transactional
    fun updateMemberRole(
        projectId: UUID,
        userId: UUID,
        targetUserId: UUID,
        request: UpdateMemberRoleRequest,
    ): MemberResponse {
        requireRole(projectId, userId, Role.OWNER)
        val member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
            ?: throw NotFoundException("Member not found")
        if (member.role == Role.OWNER && request.role != Role.OWNER) {
            ensureNotLastOwner(projectId, targetUserId)
        }
        member.role = request.role
        projectMemberRepository.save(member)
        val target = userRepository.findById(targetUserId)
            .orElseThrow { NotFoundException("User not found") }
        return MemberResponse(targetUserId, target.email, target.name, member.role, member.createdAt)
    }

    @Transactional
    fun removeMember(projectId: UUID, userId: UUID, targetUserId: UUID) {
        requireRole(projectId, userId, Role.ADMIN)
        val member = projectMemberRepository.findByProjectIdAndUserId(projectId, targetUserId)
            ?: throw NotFoundException("Member not found")
        if (member.role == Role.OWNER) {
            ensureNotLastOwner(projectId, targetUserId)
        }
        projectMemberRepository.delete(member)
    }

    @Transactional(readOnly = true)
    fun requireRole(projectId: UUID, userId: UUID, minRole: Role): ProjectMember {
        val member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
            ?: throw NotFoundException("Project not found")
        if (member.role.rank < minRole.rank) {
            throw ForbiddenException("Insufficient permissions")
        }
        return member
    }

    fun findProject(projectId: UUID): Project =
        projectRepository.findById(projectId).orElseThrow { NotFoundException("Project not found") }

    private fun ensureNotLastOwner(projectId: UUID, targetUserId: UUID) {
        val owners = projectMemberRepository.findAllByProjectIdAndRole(projectId, Role.OWNER)
        if (owners.size <= 1 && owners.any { it.userId == targetUserId }) {
            throw ConflictException("Project must have at least one owner")
        }
    }

    private fun Project.toResponse(role: Role) = ProjectResponse(
        id = requireNotNull(id),
        name = name,
        domain = domain,
        timezone = timezone,
        publicKey = publicKey,
        role = role,
        createdAt = createdAt,
    )
}

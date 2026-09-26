package com.analytics.project

import com.analytics.project.dto.AddMemberRequest
import com.analytics.project.dto.MemberResponse
import com.analytics.project.dto.UpdateMemberRoleRequest
import com.analytics.security.SecurityUtils
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
class ProjectMemberController(private val projectService: ProjectService) {

    @GetMapping
    fun list(@PathVariable projectId: UUID): List<MemberResponse> =
        projectService.listMembers(projectId, SecurityUtils.currentUserId())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: AddMemberRequest,
    ): MemberResponse = projectService.addMember(projectId, SecurityUtils.currentUserId(), request)

    @PatchMapping("/{userId}")
    fun updateRole(
        @PathVariable projectId: UUID,
        @PathVariable userId: UUID,
        @Valid @RequestBody request: UpdateMemberRoleRequest,
    ): MemberResponse = projectService.updateMemberRole(projectId, SecurityUtils.currentUserId(), userId, request)

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable projectId: UUID, @PathVariable userId: UUID) {
        projectService.removeMember(projectId, SecurityUtils.currentUserId(), userId)
    }
}

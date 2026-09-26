package com.analytics.project

import com.analytics.project.dto.CreateProjectRequest
import com.analytics.project.dto.ProjectResponse
import com.analytics.project.dto.UpdateProjectRequest
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
@RequestMapping("/api/v1/projects")
class ProjectController(private val projectService: ProjectService) {

    @GetMapping
    fun list(): List<ProjectResponse> = projectService.listForUser(SecurityUtils.currentUserId())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateProjectRequest): ProjectResponse =
        projectService.create(SecurityUtils.currentUserId(), request)

    @GetMapping("/{projectId}")
    fun get(@PathVariable projectId: UUID): ProjectResponse =
        projectService.getForUser(projectId, SecurityUtils.currentUserId())

    @PatchMapping("/{projectId}")
    fun update(
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: UpdateProjectRequest,
    ): ProjectResponse = projectService.update(projectId, SecurityUtils.currentUserId(), request)

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable projectId: UUID) {
        projectService.delete(projectId, SecurityUtils.currentUserId())
    }
}

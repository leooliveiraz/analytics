package com.analytics.project

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {

    fun findByPublicKey(publicKey: String): Project?
}

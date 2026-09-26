package com.analytics.apikey

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "api_keys")
class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(name = "project_id", nullable = false)
    var projectId: UUID = UUID.randomUUID()

    @Column(nullable = false)
    var name: String = ""

    @Column(name = "key_prefix", nullable = false)
    var keyPrefix: String = ""

    @Column(name = "key_hash", nullable = false)
    var keyHash: String = ""

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null
}

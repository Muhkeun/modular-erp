package com.modularerp.ai.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "embedding_documents",
    indexes = [
        Index(name = "idx_embedding_source", columnList = "sourceType,sourceId"),
        Index(name = "idx_embedding_tenant", columnList = "tenant_id")
    ]
)
class EmbeddingDocument(

    @Column(nullable = false, length = 50)
    val sourceType: String,

    @Column(nullable = false, length = 100)
    val sourceId: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    val chunkIndex: Int = 0,

    @Column(columnDefinition = "TEXT")
    var embeddingVector: String? = null,

    @Column(columnDefinition = "TEXT")
    var metadata: String? = null,

    @Column(nullable = false)
    var lastSyncedAt: LocalDateTime = LocalDateTime.now()

) : TenantEntity()

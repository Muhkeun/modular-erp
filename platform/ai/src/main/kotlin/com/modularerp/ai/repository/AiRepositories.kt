package com.modularerp.ai.repository

import com.modularerp.ai.domain.AiConversation
import com.modularerp.ai.domain.AiMessage
import com.modularerp.ai.domain.ConversationStatus
import com.modularerp.ai.domain.EmbeddingDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AiConversationRepository : JpaRepository<AiConversation, Long> {

    fun findBySessionId(sessionId: String): AiConversation?

    fun findByUserIdAndStatusOrderByLastMessageAtDesc(
        userId: String,
        status: ConversationStatus,
        pageable: Pageable
    ): Page<AiConversation>

    fun findByUserIdOrderByLastMessageAtDesc(
        userId: String,
        pageable: Pageable
    ): Page<AiConversation>
}

@Repository
interface AiMessageRepository : JpaRepository<AiMessage, Long> {

    fun findByConversationIdOrderByCreatedAtAsc(conversationId: Long): List<AiMessage>

    @Query("SELECT m FROM AiMessage m WHERE m.conversation.sessionId = :sessionId ORDER BY m.createdAt ASC")
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: String): List<AiMessage>
}

@Repository
interface EmbeddingDocumentRepository : JpaRepository<EmbeddingDocument, Long> {

    fun findBySourceTypeAndSourceId(sourceType: String, sourceId: String): List<EmbeddingDocument>

    fun findByTenantIdAndSourceType(tenantId: String, sourceType: String): List<EmbeddingDocument>

    @Query(
        """SELECT e FROM EmbeddingDocument e
           WHERE e.tenantId = :tenantId
           AND LOWER(e.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
           ORDER BY e.lastSyncedAt DESC"""
    )
    fun searchByKeyword(tenantId: String, keyword: String, pageable: Pageable): Page<EmbeddingDocument>

    fun deleteBySourceTypeAndSourceId(sourceType: String, sourceId: String)
}

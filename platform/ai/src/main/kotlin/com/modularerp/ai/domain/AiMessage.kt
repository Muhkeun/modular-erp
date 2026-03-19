package com.modularerp.ai.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ai_messages")
class AiMessage(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: MessageRole,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    val tokenCount: Int? = null,

    @Column(columnDefinition = "TEXT")
    val metadata: String? = null,

    @Column(name = "message_created_at", nullable = false)
    val messageCreatedAt: LocalDateTime = LocalDateTime.now()

) : TenantEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    var conversation: AiConversation? = null
        private set

    fun assignConversation(conversation: AiConversation) {
        this.conversation = conversation
    }
}

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

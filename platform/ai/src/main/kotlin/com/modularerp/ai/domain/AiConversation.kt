package com.modularerp.ai.domain

import com.modularerp.core.domain.TenantEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ai_conversations")
class AiConversation(

    @Column(nullable = false, unique = true, length = 64)
    val sessionId: String,

    @Column(nullable = false, length = 100)
    val userId: String,

    @Column(length = 200)
    var title: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ConversationStatus = ConversationStatus.ACTIVE,

    @Column(nullable = false)
    var messageCount: Int = 0,

    var lastMessageAt: LocalDateTime? = null

) : TenantEntity() {

    @OneToMany(mappedBy = "conversation", cascade = [CascadeType.ALL], orphanRemoval = true)
    val messages: MutableList<AiMessage> = mutableListOf()

    fun addMessage(message: AiMessage) {
        messages.add(message)
        message.assignConversation(this)
        messageCount++
        lastMessageAt = LocalDateTime.now()
        if (title == null && message.role == MessageRole.USER) {
            title = message.content.take(100)
        }
    }

    fun archive() {
        status = ConversationStatus.ARCHIVED
    }
}

enum class ConversationStatus {
    ACTIVE, ARCHIVED
}

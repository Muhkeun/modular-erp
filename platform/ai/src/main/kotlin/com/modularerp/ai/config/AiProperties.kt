package com.modularerp.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "modular-erp.ai")
data class AiProperties(
    val enabled: Boolean = false,
    val provider: String = "anthropic",
    val apiKey: String = "",
    val model: String = "claude-sonnet-4-20250514",
    val maxTokens: Int = 4096,
    val temperature: Double = 0.3,
    val embedding: EmbeddingProperties = EmbeddingProperties(),
    val rag: RagProperties = RagProperties()
)

data class EmbeddingProperties(
    val enabled: Boolean = false,
    val chunkSize: Int = 500,
    val chunkOverlap: Int = 50
)

data class RagProperties(
    val enabled: Boolean = false,
    val maxResults: Int = 5,
    val minScore: Double = 0.7
)

package com.modularerp.ai.service

import com.modularerp.ai.config.AiProperties
import com.modularerp.ai.domain.EmbeddingDocument
import com.modularerp.ai.repository.EmbeddingDocumentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * RAG embedding service.
 * MVP mode: uses keyword-based TF-IDF-like matching.
 * When embedding API is enabled, uses LangChain4j embedding model.
 */
@Service
class EmbeddingService(
    private val embeddingDocumentRepository: EmbeddingDocumentRepository,
    private val aiProperties: AiProperties
) {

    @Transactional
    fun indexDocument(sourceType: String, sourceId: String, content: String, tenantId: String) {
        // Remove existing chunks for this source
        embeddingDocumentRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId)

        val chunkSize = aiProperties.embedding.chunkSize
        val chunkOverlap = aiProperties.embedding.chunkOverlap

        val chunks = chunkContent(content, chunkSize, chunkOverlap)

        chunks.forEachIndexed { index, chunk ->
            val doc = EmbeddingDocument(
                sourceType = sourceType,
                sourceId = sourceId,
                content = chunk,
                chunkIndex = index,
                lastSyncedAt = LocalDateTime.now()
            )
            doc.assignTenant(tenantId)
            embeddingDocumentRepository.save(doc)
        }
    }

    fun searchSimilar(query: String, tenantId: String, maxResults: Int? = null): List<EmbeddingDocument> {
        val limit = maxResults ?: aiProperties.rag.maxResults
        val keywords = extractKeywords(query)

        if (keywords.isEmpty()) return emptyList()

        // MVP: keyword-based search using the most significant keyword
        val primaryKeyword = keywords.maxByOrNull { it.length } ?: return emptyList()
        val results = embeddingDocumentRepository.searchByKeyword(
            tenantId, primaryKeyword, PageRequest.of(0, limit)
        )

        // Score and rank by keyword overlap
        return results.content
            .map { doc -> doc to scoreDocument(doc.content, keywords) }
            .filter { it.second >= aiProperties.rag.minScore }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    @Transactional
    fun syncModuleData(moduleCode: String, tenantId: String, documents: List<Pair<String, String>>) {
        documents.forEach { (sourceId, content) ->
            indexDocument(moduleCode, sourceId, content, tenantId)
        }
    }

    private fun chunkContent(content: String, chunkSize: Int, overlap: Int): List<String> {
        if (content.length <= chunkSize) return listOf(content)

        val chunks = mutableListOf<String>()
        var start = 0
        while (start < content.length) {
            val end = minOf(start + chunkSize, content.length)
            chunks.add(content.substring(start, end))
            start += chunkSize - overlap
        }
        return chunks
    }

    private fun extractKeywords(query: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "이", "가", "을", "를", "에", "의", "로", "와", "과", "도",
            "은", "는", "에서", "으로", "하다", "되다", "있다", "없다",
            "해줘", "보여줘", "알려줘", "뭐야", "어떻게"
        )
        return query.split(Regex("[\\s,;.!?]+"))
            .map { it.trim().lowercase() }
            .filter { it.length > 1 && it !in stopWords }
    }

    private fun scoreDocument(content: String, keywords: List<String>): Double {
        val lowerContent = content.lowercase()
        val matchCount = keywords.count { lowerContent.contains(it) }
        return if (keywords.isEmpty()) 0.0 else matchCount.toDouble() / keywords.size
    }
}

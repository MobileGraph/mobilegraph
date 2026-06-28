package io.mobilegraph.tools.selection

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.tools.EmbeddingStore
import io.mobilegraph.core.tools.InMemoryEmbeddingStore
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.core.tools.ToolSelector
import io.mobilegraph.models.EmbeddingModel

/**
 * A ToolSelector that uses semantic search (embeddings) to find relevant tools.
 * It uses an [EmbeddingStore] to cache tool embeddings.
 */
class SemanticToolSelector(
    private val embeddingModel: EmbeddingModel,
    private val embeddingStore: EmbeddingStore = InMemoryEmbeddingStore(),
    private val topK: Int = 5,
    private val threshold: Float = 0.85f,
) : ToolSelector {
    override suspend fun selectTools(
        query: String,
        registry: ToolRegistry,
        context: ExecutionContext,
    ): List<Tool<*, *>> {
        val tools = registry.getAll()
        if (tools.isEmpty()) return emptyList()

        val queryEmbedding = embeddingModel.embed(query, context)

        return tools
            .map { tool ->
                val toolEmbedding = getOrComputeEmbedding(tool, context)
                tool to cosineSimilarity(queryEmbedding, toolEmbedding)
            }.filter {
                it.second >= threshold
            }.sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private suspend fun getOrComputeEmbedding(
        tool: Tool<*, *>,
        context: ExecutionContext,
    ): FloatArray {
        val key = "tool:${tool.metadata.name}"
        return embeddingStore.get(key) ?: run {
            val embedding = embeddingModel.embed("${tool.metadata.name}: ${tool.metadata.description}", context)
            embeddingStore.put(key, embedding)
            embedding
        }
    }

    private fun cosineSimilarity(
        v1: FloatArray,
        v2: FloatArray,
    ): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator == 0.0f) 0.0f else dotProduct / denominator
    }
}

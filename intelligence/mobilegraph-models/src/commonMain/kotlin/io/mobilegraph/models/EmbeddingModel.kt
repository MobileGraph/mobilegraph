package io.mobilegraph.models

import io.mobilegraph.core.context.ExecutionContext

/**
 * Interface for models that generate embeddings.
 */
interface EmbeddingModel : Model {
    /**
     * Generates an embedding for a given text.
     */
    suspend fun embed(
        text: String,
        context: ExecutionContext,
    ): List<Float>

    /**
     * Generates embeddings for a list of texts.
     */
    suspend fun embedBatch(
        texts: List<String>,
        context: ExecutionContext,
    ): List<List<Float>>
}

package io.mobilegraph.core.tools

/**
 * Interface for storing and retrieving embeddings.
 */
interface EmbeddingStore {
    /**
     * Retrieves an embedding by its key.
     */
    fun get(key: String): List<Float>?

    /**
     * Stores an embedding with the given key.
     */
    fun put(
        key: String,
        embedding: List<Float>,
    )

    /**
     * Retrieves all stored embeddings.
     */
    fun getAll(): Map<String, List<Float>>
}

/**
 * A simple in-memory implementation of [EmbeddingStore].
 */
class InMemoryEmbeddingStore : EmbeddingStore {
    private val storage = mutableMapOf<String, List<Float>>()

    override fun get(key: String): List<Float>? = storage[key]

    override fun put(
        key: String,
        embedding: List<Float>,
    ) {
        storage[key] = embedding
    }

    override fun getAll(): Map<String, List<Float>> = storage.toMap()
}

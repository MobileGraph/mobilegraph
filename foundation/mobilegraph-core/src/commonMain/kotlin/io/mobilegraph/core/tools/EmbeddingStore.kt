package io.mobilegraph.core.tools

/**
 * Interface for storing and retrieving embeddings.
 */
interface EmbeddingStore {
    /**
     * Retrieves an embedding by its key.
     */
    fun get(key: String): FloatArray?

    /**
     * Stores an embedding with the given key.
     */
    fun put(
        key: String,
        embedding: FloatArray,
    )

    /**
     * Retrieves all stored embeddings.
     */
    fun getAll(): Map<String, FloatArray>
}

/**
 * A simple in-memory implementation of [EmbeddingStore].
 */
class InMemoryEmbeddingStore : EmbeddingStore {
    private val storage = mutableMapOf<String, FloatArray>()

    override fun get(key: String): FloatArray? = storage[key]

    override fun put(
        key: String,
        embedding: FloatArray,
    ) {
        storage[key] = embedding
    }

    override fun getAll(): Map<String, FloatArray> = storage.toMap()
}

package io.mobilegraph.retrieval

/**
 * A registry for managing and discovering [Retriever] implementations at runtime.
 *
 * This allow the SDK to support multiple retrieval strategies (e.g., "fast-bm25", "accurate-vector")
 * and switch between them dynamically.
 */
interface RetrieverRegistry {
    /**
     * Retrieves a named retriever from the registry.
     */
    fun get(name: String): Retriever?

    /**
     * Returns the default retriever configured for the environment.
     */
    fun default(): Retriever?
}

/**
 * Default implementation of [RetrieverRegistry].
 */
class DefaultRetrieverRegistry : RetrieverRegistry {
    private val retrievers = mutableMapOf<String, Retriever>()
    private var defaultRetrieverName: String? = null

    /**
     * Registers a new retriever.
     *
     * @param name A unique name for the retriever.
     * @param retriever The retriever instance.
     * @param isDefault If true, this retriever becomes the primary one returned by [default].
     */
    fun register(
        name: String,
        retriever: Retriever,
        isDefault: Boolean = false,
    ) {
        retrievers[name] = retriever
        if (isDefault || defaultRetrieverName == null) {
            defaultRetrieverName = name
        }
    }

    override fun get(name: String): Retriever? = retrievers[name]

    override fun default(): Retriever? = defaultRetrieverName?.let { retrievers[it] }
}

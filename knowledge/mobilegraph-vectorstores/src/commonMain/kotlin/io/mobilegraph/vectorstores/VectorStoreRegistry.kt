package io.mobilegraph.vectorstores

/**
 * Registry for managing and discovering vector stores at runtime.
 */
interface VectorStoreRegistry {
    fun get(name: String): VectorStore?

    fun default(): VectorStore?
}

/**
 * Default implementation of [VectorStoreRegistry].
 */
class DefaultVectorStoreRegistry : VectorStoreRegistry {
    private val stores = mutableMapOf<String, VectorStore>()
    private var defaultStoreName: String? = null

    fun register(
        name: String,
        store: VectorStore,
        isDefault: Boolean = false,
    ) {
        stores[name] = store
        if (isDefault || defaultStoreName == null) {
            defaultStoreName = name
        }
    }

    override fun get(name: String): VectorStore? = stores[name]

    override fun default(): VectorStore? = defaultStoreName?.let { stores[it] }
}

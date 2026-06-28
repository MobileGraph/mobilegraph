package io.mobilegraph.vectorstores.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.vectorstores.DefaultVectorStoreRegistry
import io.mobilegraph.vectorstores.VectorStore
import io.mobilegraph.vectorstores.VectorStoreRegistry

/**
 * DSL builder for configuring [VectorStore]s during SDK initialization.
 *
 * This allows you to register one or more vector stores (e.g., SQLite, In-Memory)
 * and designate a default one for general use.
 */
class VectorStoresBuilder {
    private val registry = DefaultVectorStoreRegistry()

    /**
     * Registers a vector store with a unique name.
     *
     * @param name The unique identifier for this store.
     * @param store The [VectorStore] instance.
     * @param isDefault If true, this store will be used when no specific store is requested.
     */
    fun register(
        name: String,
        store: VectorStore,
        isDefault: Boolean = false,
    ) {
        registry.register(name, store, isDefault)
    }

    internal fun build(): VectorStoreRegistry = registry
}

/**
 * Extension to configure vector stores within the [MobileGraphEnvironment] builder.
 *
 * @param block Configuration block for [VectorStoresBuilder].
 */
fun MobileGraphEnvironment.Builder.withVectorStores(block: VectorStoresBuilder.() -> Unit) =
    apply {
        val builder = VectorStoresBuilder()
        builder.block()
        component(VectorStoreRegistry::class, builder.build())
    }

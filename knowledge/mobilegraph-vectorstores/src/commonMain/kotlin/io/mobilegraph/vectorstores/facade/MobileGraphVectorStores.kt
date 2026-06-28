package io.mobilegraph.vectorstores.facade

import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.vectorstores.VectorStore
import io.mobilegraph.vectorstores.VectorStoreRegistry

/**
 * Provides access to vector store functionality through the MobileGraph facade.
 */
val MobileGraph.vectorStores: MobileGraphVectorStores
    get() = MobileGraphVectorStores(this)

val MobileGraph.Companion.vectorStores: MobileGraphVectorStores
    get() = MobileGraph.instance.vectorStores

class MobileGraphVectorStores(
    private val mobileGraph: MobileGraph,
) {
    fun default(): VectorStore = registry().default() ?: throw IllegalStateException("No default VectorStore registered")

    fun registry(): VectorStoreRegistry =
        mobileGraph.getComponent(VectorStoreRegistry::class)
            ?: throw IllegalStateException("VectorStoreRegistry not found in environment")
}

package io.mobilegraph.retrieval.facade

import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.retrieval.Retriever
import io.mobilegraph.retrieval.RetrieverRegistry

/**
 * Provides access to retrieval functionality through the MobileGraph facade.
 */
val MobileGraph.retrieval: MobileGraphRetrieval
    get() = MobileGraphRetrieval(this)

val MobileGraph.Companion.retrieval: MobileGraphRetrieval
    get() = MobileGraph.instance.retrieval

class MobileGraphRetrieval(
    private val mobileGraph: MobileGraph,
) {
    fun default(): Retriever = registry().default() ?: throw IllegalStateException("No default Retriever registered")

    fun registry(): RetrieverRegistry =
        mobileGraph.getComponent(RetrieverRegistry::class)
            ?: throw IllegalStateException("RetrieverRegistry not found in environment")
}

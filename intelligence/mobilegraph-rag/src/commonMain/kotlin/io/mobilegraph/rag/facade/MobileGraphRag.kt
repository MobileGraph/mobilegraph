package io.mobilegraph.rag.facade

import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.rag.RagPipeline

/**
 * Accessor for RAG-related functionality via the [MobileGraph] instance.
 */
val MobileGraph.rag: MobileGraphRag
    get() = MobileGraphRag(this)

/**
 * Static accessor for RAG-related functionality via the [MobileGraph] companion.
 */
val MobileGraph.Companion.rag: MobileGraphRag
    get() = MobileGraph.instance.rag

/**
 * Provides access to RAG components, primarily the [RagPipeline].
 */
class MobileGraphRag(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Returns the active [RagPipeline] configured in the environment.
     *
     * @throws IllegalStateException if no pipeline has been configured.
     */
    fun pipeline(): RagPipeline =
        mobileGraph.getComponent(RagPipeline::class)
            ?: throw IllegalStateException("RagPipeline not found in environment. Did you call withRag { ... } during initialization?")
}

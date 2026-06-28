package io.mobilegraph.retrieval.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.retrieval.DefaultRetrieverRegistry
import io.mobilegraph.retrieval.Retriever
import io.mobilegraph.retrieval.RetrieverRegistry

/**
 * DSL builder for configuring [Retriever]s during SDK initialization.
 *
 * This allows you to register multiple retrieval strategies (e.g., "hybrid", "bm25")
 * and define which one should be used by default in the RAG pipeline.
 */
class RetrieversBuilder {
    private val registry = DefaultRetrieverRegistry()

    /**
     * Registers a retriever with a unique name.
     *
     * @param name The unique identifier for this retriever.
     * @param retriever The [Retriever] instance.
     * @param isDefault If true, this retriever will be used when no specific one is requested.
     */
    fun register(
        name: String,
        retriever: Retriever,
        isDefault: Boolean = false,
    ) {
        registry.register(name, retriever, isDefault)
    }

    internal fun build(): RetrieverRegistry = registry
}

/**
 * Extension to configure retrievers within the [MobileGraphEnvironment] builder.
 *
 * @param block Configuration block for [RetrieversBuilder].
 */
fun MobileGraphEnvironment.Builder.withRetrievers(block: RetrieversBuilder.() -> Unit) =
    apply {
        val builder = RetrieversBuilder()
        builder.block()
        component(RetrieverRegistry::class, builder.build())
    }

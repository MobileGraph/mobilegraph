package io.mobilegraph.rag.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.models.ChatModel
import io.mobilegraph.rag.ContextBasedPromptBuilder
import io.mobilegraph.rag.DefaultContextBasedPromptBuilder
import io.mobilegraph.rag.RagPipeline
import io.mobilegraph.rag.SimpleRagPipeline
import io.mobilegraph.retrieval.Retriever

/**
 * DSL for configuring a RAG (Retrieval-Augmented Generation) pipeline.
 */
class RagPipelineBuilder {
    private var retriever: Retriever? = null
    private var chatModel: ChatModel? = null
    private var contextBasedPromptBuilder: ContextBasedPromptBuilder = DefaultContextBasedPromptBuilder()

    /**
     * Sets the [Retriever] to be used for fetching relevant documents.
     *
     * The retriever is the "Search" component of RAG. It is responsible for finding
     * the most relevant snippets from your knowledge base (Vector Store, BM25, etc.)
     * based on the user's query.
     *
     * @param retriever The retriever implementation.
     */
    fun retriever(retriever: Retriever) {
        this.retriever = retriever
    }

    /**
     * Sets the [ChatModel] that will generate the final response.
     *
     * The chat model is the "Generation" component of RAG. It takes the retrieved
     * context and the user's question to produce a natural language answer.
     *
     * @param chatModel The LLM implementation (e.g., OpenAI, Gemini).
     */
    fun chatModel(chatModel: ChatModel) {
        this.chatModel = chatModel
    }

    /**
     * Sets a custom [ContextBasedPromptBuilder] to format the combined context and query.
     *
     * This component is responsible for the "Augmentation" part of RAG. It defines
     * how retrieved documents are formatted and what instructions (guardrails) are
     * given to the model (e.g., "Only use the provided context to answer").
     *
     * Defaults to [DefaultContextBasedPromptBuilder] if not specified.
     *
     * @param contextBasedPromptBuilder The prompt builder implementation.
     */
    fun contextBuilder(contextBasedPromptBuilder: ContextBasedPromptBuilder) {
        this.contextBasedPromptBuilder = contextBasedPromptBuilder
    }

    internal fun build(): RagPipeline {
        val r = retriever ?: throw IllegalStateException("Retriever must be configured for RagPipeline")
        val m = chatModel ?: throw IllegalStateException("ChatModel must be configured for RagPipeline")
        return SimpleRagPipeline(r, contextBasedPromptBuilder, m)
    }
}

/**
 * Configures a default RAG pipeline for the MobileGraph environment.
 */
fun MobileGraphEnvironment.Builder.withRag(block: RagPipelineBuilder.() -> Unit) =
    apply {
        val builder = RagPipelineBuilder()
        builder.block()
        component(RagPipeline::class, builder.build())
    }

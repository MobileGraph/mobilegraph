package io.mobilegraph.rag

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.documents.Document
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import io.mobilegraph.retrieval.Retriever

/**
 * Interface for a RAG (Retrieval-Augmented Generation) pipeline.
 *
 * A RAG pipeline automates the process of fetching relevant context and using it to
 * augment an LLM prompt, providing more accurate and grounded answers.
 */
interface RagPipeline {
    /**
     * Executes the full RAG cycle: retrieval, prompt augmentation, and model invocation.
     *
     * @param query The user's natural language question.
     * @param executionContext The context for observability and cancellation.
     * @return The response from the LLM.
     */
    suspend fun execute(
        query: String,
        executionContext: ExecutionContext = ExecutionContext.Empty,
    ): ModelOutput

    /**
     * Executes the RAG cycle using a pre-retrieved set of documents.
     *
     * Use this method when you have already performed retrieval and want to use
     * a specific set of documents for augmentation.
     *
     * @param query The user's question.
     * @param docContext The list of documents to use as context.
     * @param executionContext The execution context.
     */
    suspend fun execute(
        query: String,
        docContext: List<Document>?,
        executionContext: ExecutionContext,
    ): ModelOutput

    /**
     * Executes the RAG pipeline and returns a [RagExecutionResult] containing
     * detailed debugging information.
     *
     * @param query The user's question.
     * @param executionContext The execution context.
     */
    suspend fun executeDetailed(
        query: String,
        executionContext: ExecutionContext = ExecutionContext.Empty,
    ): RagExecutionResult
}

/**
 * A standard implementation of [RagPipeline].
 *
 * This implementation coordinates between a [Retriever] to find context, a
 * [ContextBasedPromptBuilder] to format the prompt, and a [ChatModel] to generate the answer.
 */
class SimpleRagPipeline(
    private val retriever: Retriever,
    private val contextBasedPromptBuilder: ContextBasedPromptBuilder,
    private val chatModel: ChatModel,
) : RagPipeline {
    override suspend fun execute(
        query: String,
        executionContext: ExecutionContext,
    ): ModelOutput = executeDetailed(query, executionContext).output

    override suspend fun execute(
        query: String,
        docContext: List<Document>?,
        executionContext: ExecutionContext,
    ): ModelOutput {
        val fullPrompt = contextBasedPromptBuilder.buildContextAndPrompt(docContext, query)

        val result = chatModel.invoke(ChatPromptValue(listOf(UserMessage(fullPrompt))), context = executionContext)

        publish(
            MobileGraphEvent.RagResponseGenerated(
                traceId = executionContext.traceId,
                requestId = executionContext.requestId,
                sessionId = executionContext.sessionId,
            ),
        )

        return result
    }

    override suspend fun executeDetailed(
        query: String,
        executionContext: ExecutionContext,
    ): RagExecutionResult {
        // 1. Retrieve
        val retrievalResult = retriever.retrieveDetailed(query, context = executionContext)

        // 2. Build Context and prompt
        val fullPrompt = contextBasedPromptBuilder.buildContextAndPrompt(retrievalResult.documents, query)

        // 3. Chat Model
        val output = chatModel.invoke(ChatPromptValue(listOf(UserMessage(fullPrompt))), context = executionContext)

        // 4. Publish Event
        publish(
            MobileGraphEvent.RagResponseGenerated(
                traceId = executionContext.traceId,
                requestId = executionContext.requestId,
                sessionId = executionContext.sessionId,
            ),
        )

        return RagExecutionResult(
            output = output,
            finalPrompt = fullPrompt,
            retrievedDocs = retrievalResult.scoredDocuments,
            filteredDocs = retrievalResult.documents,
            similarityThreshold = retrievalResult.threshold,
        )
    }

    private suspend fun publish(event: MobileGraphEvent) {
        try {
            val publisher = MobileGraph.instance.getComponent(EventPublisher::class)
            publisher?.publish(event)
        } catch (e: Exception) {
            // Silently fail
        }
    }
}

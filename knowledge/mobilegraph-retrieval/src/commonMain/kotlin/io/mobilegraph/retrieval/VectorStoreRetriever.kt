package io.mobilegraph.retrieval

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.documents.Document
import io.mobilegraph.vectorstores.RetrievalFilter
import io.mobilegraph.vectorstores.VectorStore

/**
 * A [Retriever] implementation that performs semantic search using a [VectorStore].
 *
 * This retriever converts the search query into an embedding (via the VectorStore's model)
 * and finds the top-k most similar document chunks based on vector distance.
 *
 * @property vectorStore The underlying store where documents and embeddings are kept.
 * @property topK The maximum number of results to fetch from the store. Defaults to 4.
 * @property similarityThreshold An optional minimum score (0.0 to 1.0) required for a
 *                                document to be included in the results.
 */
class VectorStoreRetriever(
    private val vectorStore: VectorStore,
    private val topK: Int = 4,
    private val similarityThreshold: Float? = 0.65f,
) : Retriever {
    override suspend fun retrieve(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<Document> = retrieveDetailed(query, filter, context).documents

    override suspend fun retrieveDetailed(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): RetrievalResult {
        // Publish Started Event
        publish(
            MobileGraphEvent.RetrievalStarted(
                traceId = context.traceId,
                requestId = context.requestId,
                sessionId = context.sessionId,
                query = query,
            ),
        )

        val scoredDocs = vectorStore.similaritySearch(query, topK, filter, context)

        val filteredDocs =
            scoredDocs
                .filter { scoredDoc ->
                    similarityThreshold == null || scoredDoc.score >= similarityThreshold
                }

        // Publish Completed Event
        publish(
            MobileGraphEvent.RetrievalCompleted(
                traceId = context.traceId,
                requestId = context.requestId,
                sessionId = context.sessionId,
                filteredDocumentCount = filteredDocs.size,
                vectorSearchedResult = scoredDocs.take(3).map { it.document.id to it.score },
            ),
        )

        return RetrievalResult(
            documents = filteredDocs.map { it.document },
            scoredDocuments = scoredDocs,
            threshold = similarityThreshold,
        )
    }

    private suspend fun publish(event: MobileGraphEvent) {
        try {
            val publisher = MobileGraph.instance.getComponent(EventPublisher::class)
            publisher?.publish(event)
        } catch (e: Exception) {
            // Silently fail if SDK not initialized
        }
    }
}

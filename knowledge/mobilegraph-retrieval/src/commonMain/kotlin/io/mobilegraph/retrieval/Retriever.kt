package io.mobilegraph.retrieval

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.vectorstores.RetrievalFilter

/**
 * Interface for retrieving relevant context based on a search query.
 *
 * Retrievers are responsible for the first stage of the RAG pipeline: finding the
 * information that will be used to answer the user's question. This can be achieved
 * via vector search, keyword matching, or hybrid strategies.
 */
interface Retriever {
    /**
     * Finds and returns relevant documents based on the query string.
     *
     * @param query The user's input query.
     * @param filter An optional filter to restrict the search (e.g., to a specific document ID).
     * @param context The execution context for tracking and observability.
     * @return A list of documents deemed relevant to the query.
     */
    suspend fun retrieve(
        query: String,
        filter: RetrievalFilter? = null,
        context: ExecutionContext = ExecutionContext.Empty,
    ): List<Document>

    /**
     * Retrieves relevant documents with detailed metadata for debugging and introspection.
     *
     * @param query The search query.
     * @param filter Optional search filter.
     * @param context The execution context.
     * @return A [RetrievalResult] containing both final documents and raw scoring data.
     */
    suspend fun retrieveDetailed(
        query: String,
        filter: RetrievalFilter? = null,
        context: ExecutionContext = ExecutionContext.Empty,
    ): RetrievalResult {
        val docs = retrieve(query, filter, context)
        return RetrievalResult(docs)
    }
}

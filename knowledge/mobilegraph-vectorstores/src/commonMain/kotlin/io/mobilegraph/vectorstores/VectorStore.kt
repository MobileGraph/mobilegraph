package io.mobilegraph.vectorstores

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import kotlin.math.sqrt

/**
 * Defines criteria for filtering document retrieval in a [VectorStore].
 */
sealed interface RetrievalFilter {
    /**
     * Filters results to only include chunks belonging to a specific document ID.
     */
    data class DocumentId(
        val id: String,
    ) : RetrievalFilter

    /**
     * Filters results based on a custom metadata key-value pair.
     */
    data class Metadata(
        val key: String,
        val value: String,
    ) : RetrievalFilter
}

/**
 * Interface for storing [Document] chunks and searching them using vector embeddings.
 *
 * A [VectorStore] is the core component of semantic search. It converts documents into
 * numerical vectors (embeddings) and uses distance metrics (like Cosine Similarity) to
 * find the most relevant context for a given user query.
 */
interface VectorStore {
    /**
     * Ingests a list of documents into the store.
     *
     * This process typically involves generating embeddings for each document's content
     * before persisting them.
     *
     * @param documents The list of documents to add.
     * @param context The execution context for tracking and observability.
     */
    suspend fun add(
        documents: List<Document>,
        context: ExecutionContext = ExecutionContext.Empty,
    )

    /**
     * Removes specific document chunks from the store by their unique IDs.
     *
     * @param ids The list of chunk IDs to delete.
     * @param context The execution context.
     */
    suspend fun delete(
        ids: List<String>,
        context: ExecutionContext = ExecutionContext.Empty,
    )

    /**
     * Removes all chunks belonging to a specific document ID.
     *
     * @param documentId The ID of the parent document to remove.
     * @param context The execution context.
     */
    suspend fun deleteByDocument(
        documentId: String,
        context: ExecutionContext = ExecutionContext.Empty,
    )

    /**
     * Performs a semantic similarity search based on a string query.
     *
     * @param query The user's search query.
     * @param k The maximum number of results to return. Defaults to 4.
     * @param filter An optional [RetrievalFilter] to restrict the search space.
     * @param context The execution context.
     * @return A list of [ScoredDocument]s ranked by relevance.
     */
    suspend fun similaritySearch(
        query: String,
        k: Int = 4,
        filter: RetrievalFilter? = null,
        context: ExecutionContext = ExecutionContext.Empty,
    ): List<ScoredDocument>

    /**
     * Wipes all data from the vector store.
     *
     * @param context The execution context.
     */
    suspend fun clear(context: ExecutionContext = ExecutionContext.Empty)

    /**
     * Retrieves all unique document IDs currently indexed in the store.
     *
     * @param context The execution context.
     * @return A list of unique document identifiers.
     */
    suspend fun getAllDocumentIds(context: ExecutionContext = ExecutionContext.Empty): List<String>

    /**
     * Retrieves all document chunks currently stored in the vector store.
     *
     * @param context The execution context.
     * @return A list of all documents.
     */
    suspend fun getAllDocuments(context: ExecutionContext = ExecutionContext.Empty): List<Document>

    /**
     * Utility to calculate the cosine similarity between two vectors.
     *
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return A similarity score between 0.0 (unrelated) and 1.0 (identical).
     */
    fun cosineSimilarity(
        v1: FloatArray,
        v2: FloatArray,
    ): Float {
        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        if (norm1 == 0f || norm2 == 0f) return 0f
        return dotProduct / (sqrt(norm1) * sqrt(norm2))
    }
}

/**
 * Represents a document retrieved from a [VectorStore] with its associated relevance score.
 *
 * @property document The retrieved document chunk.
 * @property score The similarity score (usually 0.0 to 1.0).
 */
data class ScoredDocument(
    val document: Document,
    val score: Float,
)

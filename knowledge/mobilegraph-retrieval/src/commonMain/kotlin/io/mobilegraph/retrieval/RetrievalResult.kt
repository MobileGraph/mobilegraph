package io.mobilegraph.retrieval

import io.mobilegraph.documents.Document
import io.mobilegraph.vectorstores.ScoredDocument

/**
 * Encapsulates the results of a retrieval operation, including metadata for debugging.
 *
 * @property documents The final list of relevant documents to be used in context augmentation.
 * @property scoredDocuments The original raw results from the [VectorStore] before any post-processing.
 * @property threshold The similarity threshold that was applied to filter [scoredDocuments].
 */
data class RetrievalResult(
    val documents: List<Document>,
    val scoredDocuments: List<ScoredDocument> = emptyList(),
    val threshold: Float? = null,
)

package io.mobilegraph.retrieval

import io.mobilegraph.core.annotations.ExperimentalMobileGraphApi
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.vectorstores.RetrievalFilter

/**
 * A retriever that combines results from multiple [Retriever] implementations using
 * Reciprocal Rank Fusion (RRF).
 *
 * Hybrid retrieval typically combines semantic search (Vector) and keyword search (BM25)
 * to leverage the strengths of both: semantic understanding and exact term matching.
 *
 * @param retrievers The list of retrievers to consult.
 * @property k A constant used in the RRF formula to control the influence of low-ranked items.
 *             Defaults to 60.
 */
class HybridRetriever(
    private val retrievers: List<Retriever>,
    private val k: Int = 60,
) : Retriever {
    override suspend fun retrieve(
        query: String,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<Document> {
        val allResults = retrievers.map { it.retrieve(query, filter, context) }

        val rrfScores = mutableMapOf<String, Float>()
        val documentMap = mutableMapOf<String, Document>()

        allResults.forEach { docs ->
            docs.forEachIndexed { rank, doc ->
                documentMap[doc.id] = doc
                val currentScore = rrfScores[doc.id] ?: 0f
                // RRF Formula: score = sum( 1 / (k + rank + 1) )
                rrfScores[doc.id] = currentScore + (1f / (k + rank + 1))
            }
        }

        return rrfScores.entries
            .sortedByDescending { it.value }
            .map { documentMap[it.key]!! }
    }
}

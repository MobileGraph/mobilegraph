package io.mobilegraph.vectorstores

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentMetadata
import io.mobilegraph.models.EmbeddingModel
import kotlin.math.sqrt

/**
 * A simple in-memory implementation of [VectorStore].
 *
 * This implementation is volatile and suitable for temporary sessions or testing.
 * Data is lost when the [VectorStore] instance is garbage collected or the app process dies.
 *
 * @param embeddingModel The model used to generate embeddings for new documents and queries.
 */
class InMemoryVectorStore(
    private val embeddingModel: EmbeddingModel,
) : VectorStore {
    private val entries = mutableListOf<VectorEntry>()

    private data class VectorEntry(
        val document: Document,
        val embedding: FloatArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as VectorEntry

            if (document != other.document) return false
            if (!embedding.contentEquals(other.embedding)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = document.hashCode()
            result = 31 * result + embedding.contentHashCode()
            return result
        }
    }

    override suspend fun add(
        documents: List<Document>,
        context: ExecutionContext,
    ) {
        val embeddings = embeddingModel.embed(documents.map { it.content }, context)
        documents.zip(embeddings).forEach { (doc, emb) ->
            entries.add(VectorEntry(doc, emb))
        }
    }

    override suspend fun delete(
        ids: List<String>,
        context: ExecutionContext,
    ) {
        entries.removeAll { it.document.id in ids }
    }

    override suspend fun deleteByDocument(
        documentId: String,
        context: ExecutionContext,
    ) {
        entries.removeAll { it.document.metadata[DocumentMetadata.DOC_ID] == documentId }
    }

    override suspend fun similaritySearch(
        query: String,
        k: Int,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<ScoredDocument> {
        val queryEmbedding = embeddingModel.embed(query, context)

        return entries
            .asSequence()
            .filter { entry ->
                when (filter) {
                    is RetrievalFilter.DocumentId -> entry.document.metadata[DocumentMetadata.DOC_ID] == filter.id
                    is RetrievalFilter.Metadata -> entry.document.metadata[filter.key] == filter.value
                    null -> true
                }
            }.map { entry ->
                ScoredDocument(
                    document = entry.document,
                    score = cosineSimilarity(queryEmbedding, entry.embedding),
                )
            }.sortedByDescending { it.score }
            .take(k)
            .toList()
    }

    override suspend fun clear(context: ExecutionContext) {
        entries.clear()
    }

    override suspend fun getAllDocumentIds(context: ExecutionContext): List<String> =
        entries
            .mapNotNull {
                it.document.metadata[DocumentMetadata.DOC_ID]
            }.distinct()

    override suspend fun getAllDocuments(context: ExecutionContext): List<Document> = entries.map { it.document }
}

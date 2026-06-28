package io.mobilegraph.vectorstores

import app.cash.sqldelight.db.SqlDriver
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.documents.Document
import io.mobilegraph.documents.DocumentMetadata
import io.mobilegraph.models.EmbeddingModel
import io.mobilegraph.vectorstores.db.VectorDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.sqrt

/**
 * A [VectorStore] implementation that persists data using SQLite via SQLDelight.
 *
 * This store provides persistent, on-device storage for document chunks and their
 * embeddings. It supports metadata filtering and targeted retrieval by document ID.
 *
 * @param driver The SQLDelight driver used to open the database connection.
 * @param embeddingModel The model used to generate embeddings for new documents and queries.
 */
class SQLiteVectorStore(
    driver: SqlDriver,
    private val embeddingModel: EmbeddingModel,
) : VectorStore {
    private val database = VectorDatabase(driver)
    private val queries = database.vectorEntityQueries
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun add(
        documents: List<Document>,
        context: ExecutionContext,
    ) {
        val embeddings = embeddingModel.embed(documents.map { it.content }, context)
        documents.zip(embeddings).forEach { (doc, emb) ->
            queries.insert(
                chunk_id = doc.id,
                doc_id = doc.metadata[DocumentMetadata.DOC_ID] ?: "unknown",
                doc_name = doc.metadata[DocumentMetadata.DOC_NAME],
                page_number = doc.metadata[DocumentMetadata.PAGE]?.toLong(),
                content = doc.content,
                metadata = json.encodeToString(doc.metadata),
                embedding = emb.toByteArray(),
            )
        }
    }

    override suspend fun delete(
        ids: List<String>,
        context: ExecutionContext,
    ) {
        ids.forEach { queries.deleteById(it) }
    }

    override suspend fun deleteByDocument(
        documentId: String,
        context: ExecutionContext,
    ) {
        queries.deleteByDocId(documentId)
    }

    override suspend fun similaritySearch(
        query: String,
        k: Int,
        filter: RetrievalFilter?,
        context: ExecutionContext,
    ): List<ScoredDocument> {
        val queryEmbedding = embeddingModel.embed(query, context)

        val entries =
            when (filter) {
                is RetrievalFilter.DocumentId -> queries.getByDocId(filter.id).executeAsList()
                else -> queries.getAll().executeAsList()
            }

        return entries
            .asSequence()
            .map { entry ->
                val metadata: Map<String, String> = json.decodeFromString(entry.metadata)
                entry to metadata
            }.filter { (_, metadata) ->
                if (filter is RetrievalFilter.Metadata) {
                    metadata[filter.key] == filter.value
                } else {
                    true
                }
            }.map { (entry, metadata) ->
                val entryEmbedding = entry.embedding.toFloatArray()
                ScoredDocument(
                    document =
                        Document(
                            id = entry.chunk_id,
                            content = entry.content,
                            metadata = metadata,
                        ),
                    score = cosineSimilarity(queryEmbedding, entryEmbedding),
                )
            }.sortedByDescending { it.score }
            .take(k)
            .toList()
    }

    override suspend fun clear(context: ExecutionContext) {
        queries.clear()
    }

    override suspend fun getAllDocumentIds(context: ExecutionContext): List<String> = queries.getUniqueDocIds().executeAsList()

    override suspend fun getAllDocuments(context: ExecutionContext): List<Document> =
        queries.getAll().executeAsList().map { entry ->
            val metadata: Map<String, String> = json.decodeFromString(entry.metadata)
            Document(
                id = entry.chunk_id,
                content = entry.content,
                metadata = metadata,
            )
        }

    /**
     * Converts a [FloatArray] to a [ByteArray] for efficient BLOB storage in SQLite.
     */
    private fun FloatArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 4)
        for (i in indices) {
            val bits = this[i].toRawBits()
            bytes[i * 4] = (bits shr 24).toByte()
            bytes[i * 4 + 1] = (bits shr 16).toByte()
            bytes[i * 4 + 2] = (bits shr 8).toByte()
            bytes[i * 4 + 3] = bits.toByte()
        }
        return bytes
    }

    /**
     * Converts a [ByteArray] back into a [FloatArray] for similarity calculations.
     */
    private fun ByteArray.toFloatArray(): FloatArray {
        val floats = FloatArray(size / 4)
        for (i in floats.indices) {
            val bits =
                (this[i * 4].toInt() and 0xFF shl 24) or
                    (this[i * 4 + 1].toInt() and 0xFF shl 16) or
                    (this[i * 4 + 2].toInt() and 0xFF shl 8) or
                    (this[i * 4 + 3].toInt() and 0xFF)
            floats[i] = Float.fromBits(bits)
        }
        return floats
    }
}

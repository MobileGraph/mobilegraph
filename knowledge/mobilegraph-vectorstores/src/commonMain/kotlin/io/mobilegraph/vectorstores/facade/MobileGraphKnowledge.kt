package io.mobilegraph.vectorstores.facade

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.documents.DocumentLoader
import io.mobilegraph.documents.RecursiveTextSplitter
import io.mobilegraph.documents.TextSplitter
import io.mobilegraph.vectorstores.VectorStore
import io.mobilegraph.vectorstores.facade.vectorStores
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Accessor for knowledge-related functionality (Ingestion, Retrieval management).
 */
val MobileGraph.knowledge: MobileGraphKnowledge
    get() = MobileGraphKnowledge(this)

/**
 * Static accessor for knowledge functionality via the [MobileGraph] companion.
 */
val MobileGraph.Companion.knowledge: MobileGraphKnowledge
    get() = MobileGraph.instance.knowledge

/**
 * Provides access to the knowledge base, allowing for document ingestion and store management.
 */
class MobileGraphKnowledge(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Entry point for the ingestion DSL.
     *
     * Ingestion is the process of loading, splitting, and storing documents in a [VectorStore]
     * so they can be retrieved later.
     *
     * Example:
     * ```kotlin
     * MobileGraph.knowledge.ingest {
     *     docId("my-doc")
     *     loader(TextDocumentLoader("..."))
     *     splitter(RecursiveTextSplitter())
     * }
     * ```
     *
     * @param context Optional context for tracking the ingestion process.
     * @param block Configuration block for the [IngestionBuilder].
     */
    suspend fun ingest(
        context: ExecutionContext? = null,
        block: IngestionBuilder.() -> Unit,
    ) {
        val effectiveContext =
            context ?: SimpleExecutionContext(
                traceId = TraceId("ingest-trace-${Random.nextInt()}"),
                requestId = RequestId("ingest-req-${Random.nextInt()}"),
            )

        val builder = IngestionBuilder(mobileGraph, effectiveContext)
        builder.block()
        builder.execute()
    }
}

/**
 * DSL builder for configuring the ingestion of documents.
 */
class IngestionBuilder(
    private val mobileGraph: MobileGraph,
    private val context: ExecutionContext,
) {
    private var loader: DocumentLoader? = null
    private var splitter: TextSplitter = RecursiveTextSplitter()
    private var vectorStore: VectorStore? = null
    private var docId: String? = null
    private var docName: String? = null

    /** Sets the source loader for the documents. Required. */
    fun loader(loader: DocumentLoader) {
        this.loader = loader
    }

    /** Sets the strategy for splitting documents into chunks. Defaults to [RecursiveTextSplitter]. */
    fun splitter(splitter: TextSplitter) {
        this.splitter = splitter
    }

    /** Sets the target vector store. Defaults to the environment's default store. */
    fun vectorStore(vectorStore: VectorStore) {
        this.vectorStore = vectorStore
    }

    /** Sets a unique ID for the document being ingested. Recommended for targeted retrieval. */
    @OptIn(ExperimentalUuidApi::class)
    fun docId(id: Uuid) {
        this.docId = id.toString()
    }

    /** Sets a unique ID for the document being ingested. Recommended for targeted retrieval. */
    fun docId(id: String) {
        this.docId = id
    }

    /** Sets a human-readable name or title for the document. */
    fun docName(name: String) {
        this.docName = name
    }

    internal suspend fun execute() {
        val l = loader ?: throw IllegalStateException("Loader must be specified for ingestion")
        val v =
            vectorStore ?: mobileGraph.vectorStores.default()

        // 1. Load
        val rawDocs = l.load()
        val docs =
            rawDocs.map { doc ->
                val extraMetadata = mutableMapOf<String, String>()
                docId?.let { extraMetadata[io.mobilegraph.documents.DocumentMetadata.DOC_ID] = it }
                docName?.let { extraMetadata[io.mobilegraph.documents.DocumentMetadata.DOC_NAME] = it }

                if (extraMetadata.isNotEmpty()) {
                    doc.copy(metadata = doc.metadata + extraMetadata)
                } else {
                    doc
                }
            }

        publish(
            MobileGraphEvent.DocumentsLoaded(
                traceId = context.traceId,
                requestId = context.requestId,
                sessionId = context.sessionId,
                count = docs.size,
            ),
        )

        // 2. Split
        val chunks = docs.flatMap { splitter.split(it) }
        publish(
            MobileGraphEvent.ChunkingCompleted(
                traceId = context.traceId,
                requestId = context.requestId,
                sessionId = context.sessionId,
                originalCount = docs.size,
                chunkCount = chunks.size,
            ),
        )

        // 3. Store (which includes embedding)
        v.add(chunks, context)
        publish(
            MobileGraphEvent.EmbeddingsGenerated(
                traceId = context.traceId,
                requestId = context.requestId,
                sessionId = context.sessionId,
                count = chunks.size,
            ),
        )
    }

    private suspend fun publish(event: MobileGraphEvent) {
        try {
            val publisher = mobileGraph.getComponent(EventPublisher::class)
            publisher?.publish(event)
        } catch (e: Exception) {
            // Silently fail
        }
    }
}

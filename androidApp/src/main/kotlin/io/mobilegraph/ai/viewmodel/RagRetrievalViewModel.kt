package io.mobilegraph.ai.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.mobilegraph.ai.ApplicationLogger
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.core.annotations.ExperimentalMobileGraphApi
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.facade.events
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.facade.chat
import io.mobilegraph.models.facade.embedding
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.mediapipe.MediaPipeEmbeddingModel
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.parsers.asText
import io.mobilegraph.rag.facade.rag
import io.mobilegraph.rag.facade.withRag
import io.mobilegraph.retrieval.BM25Retriever
import io.mobilegraph.retrieval.HybridRetriever
import io.mobilegraph.retrieval.MultiQueryRetriever
import io.mobilegraph.retrieval.VectorStoreRetriever
import io.mobilegraph.retrieval.facade.withRetrievers
import io.mobilegraph.vectorstores.SQLiteVectorStore
import io.mobilegraph.vectorstores.db.VectorDatabase
import io.mobilegraph.vectorstores.facade.withVectorStores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel dedicated to handling knowledge retrieval and the RAG pipeline execution.
 */
@OptIn(ExperimentalMobileGraphApi::class)
class RagRetrievalViewModel : ViewModel() {
    private var mobileGraph: MobileGraph? = null
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var currentOutput by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val _availableDocIds = MutableStateFlow<List<String>>(emptyList())
    val availableDocIds: StateFlow<List<String>> = _availableDocIds

    private val openAiApiKey = BuildConfig.OPEN_AI_API_KEY

    /**
     * SDK Action: MobileGraph.initialize
     * Sets up the environment with Models, VectorStores, and Retrievers.
     */
    fun initializeSdk(context: Context) {
        if (mobileGraph != null) return

        val embeddingModel = MediaPipeEmbeddingModel({ context })

        val driver =
            AndroidSqliteDriver(
                schema = VectorDatabase.Schema,
                context = context,
                name = "mobilegraph_vector.db",
            )

        val vectorStore = SQLiteVectorStore(driver, embeddingModel)
        val vectorRetriever = VectorStoreRetriever(vectorStore)
        val chatModel = OpenAIChatModel(apiKey = openAiApiKey, name = "gpt-4o")

        mobileGraph =
            MobileGraph.initialize {
                // SDK USE: withModels DSL to register chat and embedding models
                withModels {
                    chat("gpt-4o", chatModel) {
                        isDefault = true
                        defaultConfig {
                            temperature = 0f
                            maxTokens = 200
                        }
                        middleware { +LoggingMiddleware(ApplicationLogger()) }
                    }
                    embedding(embeddingModel, isDefault = true)
                }

                // SDK USE: withVectorStores DSL to persist embeddings on-device
                withVectorStores {
                    register("sqlite", vectorStore, isDefault = true)
                }

                // SDK USE: withRetrievers DSL to register multiple retrieval strategies
                withRetrievers {
                    register("vector", vectorRetriever, isDefault = true)
                    register("bm25", BM25Retriever(emptyList()))
                    register("hybrid", HybridRetriever(listOf(vectorRetriever, BM25Retriever(emptyList()))))
                    register("multi-query", MultiQueryRetriever(vectorRetriever, chatModel))
                }

                // SDK USE: withRag DSL to configure the default RAG pipeline
                withRag {
                    retriever(vectorRetriever)
                    chatModel(chatModel)
                }
            }

        // SDK USE: MobileGraph.events to observe SDK activity
        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.RetrievalStarted -> "Retrieving context for: ${event.query}"
                        is MobileGraphEvent.RetrievalCompleted -> "Retrieved ${event.filteredDocumentCount} document chunks"
                        is MobileGraphEvent.EmbeddingsGenerated -> "Generated ${event.count} embeddings"
                        is MobileGraphEvent.RagResponseGenerated -> "LLM response generated"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }

        uiState = "SDK Initialized"
        addEvent("SDK Initialized")
        refreshDocIds()
    }

    // --- Search Methods ---

    fun searchWithVector(
        query: String,
        docId: String?,
    ) = search("vector", query, docId)

    fun searchWithBM25(
        query: String,
        docId: String?,
    ) = search("bm25", query, docId)

    fun searchWithHybrid(
        query: String,
        docId: String?,
    ) = search("hybrid", query, docId)

    fun searchWithMultiQuery(
        query: String,
        docId: String?,
    ) = search("multi-query", query, docId)

    /**
     * Core search logic that executes a retriever and passes context to the RAG pipeline.
     */
    private fun search(
        retrieverName: String,
        query: String,
        targetDocId: String?,
    ) {
        viewModelScope.launch {
            if (query.isBlank()) return@launch
            isLoading = true
            uiState = "Searching with $retrieverName..."
            try {
                val context =
                    SimpleExecutionContext(
                        traceId = TraceId("trace-${Random.nextInt()}"),
                        requestId = RequestId("req-${Random.nextInt()}"),
                    )

                // SDK USE: RetrievalFilter for targeted document search
                val filter =
                    targetDocId?.takeIf { it.isNotBlank() }?.let {
                        io.mobilegraph.vectorstores.RetrievalFilter
                            .DocumentId(it)
                    }

                // SDK USE: RetrieverRegistry to fetch the specific strategy
                val retrieverRegistry = MobileGraph.instance.getComponent(io.mobilegraph.retrieval.RetrieverRegistry::class)
                val retriever = retrieverRegistry?.get(retrieverName) ?: throw IllegalStateException("Retriever $retrieverName not found")

                // SDK ACTION: retriever.retrieve performs semantic or keyword search
                val docs = retriever.retrieve(query, filter = filter, context = context)

                // SDK ACTION: MobileGraph.rag.pipeline().execute runs the Search -> Augment -> Generate cycle
                val result = MobileGraph.rag.pipeline().execute(query, docs, context)
                currentOutput = "Retriever: $retrieverName\n\n${result.asText()}"

                uiState = "Search Completed"
                addEvent("Search '$query' using $retrieverName")
            } catch (e: Exception) {
                uiState = "Search Failed: ${e.message}"
                addEvent("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }

    /**
     * Refreshes the list of available document IDs from the VectorStore.
     */
    fun refreshDocIds() {
        viewModelScope.launch {
            try {
                // SDK USE: VectorStoreRegistry to access document inventory
                val store = MobileGraph.instance.getComponent(io.mobilegraph.vectorstores.VectorStoreRegistry::class)?.default()
                _availableDocIds.value = store?.getAllDocumentIds() ?: emptyList()

                // Refresh dynamic indices (BM25)
                updateRetrievers()
            } catch (e: Exception) {
                Log.e("RagRetrievalVM", "Error refreshing doc IDs", e)
            }
        }
    }

    /**
     * Updates retrievers that depend on the global document set (like BM25).
     */
    private suspend fun updateRetrievers() {
        val store = MobileGraph.instance.getComponent(io.mobilegraph.vectorstores.VectorStoreRegistry::class)?.default() ?: return
        val registry =
            MobileGraph.instance.getComponent(
                io.mobilegraph.retrieval.RetrieverRegistry::class,
            ) as? io.mobilegraph.retrieval.DefaultRetrieverRegistry
                ?: return

        val allDocs = store.getAllDocuments()
        if (allDocs.isEmpty()) return

        // SDK USE: Dynamically re-registering retrievers when knowledge base changes
        val bm25 = BM25Retriever(allDocs)
        val vectorRetriever = registry.get("vector") ?: VectorStoreRetriever(store)

        registry.register("bm25", bm25)
        registry.register("hybrid", HybridRetriever(listOf(vectorRetriever, bm25)))

        addEvent("Retrievers updated with ${allDocs.size} chunks")
    }
}

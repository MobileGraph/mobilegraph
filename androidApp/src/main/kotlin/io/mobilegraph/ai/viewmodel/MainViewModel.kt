/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.ai.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mobilegraph.ai.ApplicationLogger
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.memory.memory
import io.mobilegraph.core.session.MobileGraphSession
import io.mobilegraph.core.tools.AllToolSelector
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.models.facade.chat
import io.mobilegraph.models.facade.embedding
import io.mobilegraph.models.facade.model
import io.mobilegraph.models.facade.models
import io.mobilegraph.models.facade.stream
import io.mobilegraph.models.facade.useSummaryBufferMemory
import io.mobilegraph.models.facade.withMemory
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.mediapipe.MediaPipeEmbeddingModel
import io.mobilegraph.models.middleware.ChatMemoryMiddleware
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.models.middleware.RetryMiddleware
import io.mobilegraph.models.middleware.ToolSelectionMiddleware
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.parsers.ParseResult
import io.mobilegraph.parsers.asText
import io.mobilegraph.parsers.structuredOutputParser
import io.mobilegraph.prompts.composition.promptComposer
import io.mobilegraph.prompts.facade.withPrompts
import io.mobilegraph.tools.facade.withToolSelector
import io.mobilegraph.tools.facade.withTools
import io.mobilegraph.tools.selection.JsonEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ProductReview(
    val rating: Int,
    val sentiment: String,
    val summary: String,
    val keyPoints: List<String>,
)

/**
 * ViewModel for the Sample App demonstrating session-aware AI interactions.
 */
class MainViewModel : ViewModel() {
    private var mobileGraph: MobileGraph? = null

    // Manage multiple sessions
    private val sessionMap = mutableMapOf<String, MobileGraphSession>()
    var currentSessionId by mutableStateOf("Session 1")

    // UI State
    var uiState by mutableStateOf<String>("Ready")
    var isLoading by mutableStateOf(false)
    var currentOutput by mutableStateOf<String>("")
    var currentSequenceItem by mutableStateOf<String>("")
    val eventLog = mutableStateListOf<String>()

    private val openAiApiKey = BuildConfig.OPEN_AI_API_KEY

    /**
     * Initializes the SDK with a professional configuration.
     */
    fun initializeSdk(context: Context) {
        if (mobileGraph != null) return // Prevent duplicate initialization

        val filesDir = context.filesDir
        val myStore =
            JsonEmbeddingStore(
                initialData = loadCacheFromFile(filesDir),
                onSave = { json -> saveCacheToFile(filesDir, json) },
            )
        val embeddingModel = MediaPipeEmbeddingModel({ context })
        val miniModel = OpenAIChatModel(apiKey = openAiApiKey, name = "gpt-4o-mini")
        mobileGraph =
            MobileGraph.initialize {
                withTools {
                    register(weatherTool)
                }
                withToolSelector(
                    selector = AllToolSelector(),
                       /* SemanticToolSelector(
                            embeddingModel = embeddingModel,
                            embeddingStore = myStore,
                        )*/
                )
                withModels {
                    // Register a high-power model for complex tasks
                    val modelGpt4o = OpenAIChatModel(apiKey = openAiApiKey, name = "gpt-4o")
                    chat("gpt-4o", modelGpt4o) {
                        isDefault = true
                        middleware {
                            +LoggingMiddleware(ApplicationLogger())
                            +RetryMiddleware(maxRetries = 3)
                            +ToolSelectionMiddleware()
                            +ChatMemoryMiddleware()
                        }
                        defaultConfig {
                            maxTokens = 512
                            temperature = 0.5f
                        }
                    }

                    // Register a smaller model for faster/simpler tasks
                    chat("gpt-4o-mini", miniModel) {
                        middleware {
                            +LoggingMiddleware()
                            +ChatMemoryMiddleware()
                        }
                        defaultConfig {
                            maxTokens = 200
                            temperature = 0.2f
                        }
                    }

                    embedding(embeddingModel, isDefault = true)
                }
                withPrompts {
                    register("helpful", "You are a helpful AI Assistant.")
                }
                withMemory {
                    useSummaryBufferMemory(
                        modelName = "gpt-4o-mini",
                        maxBufferMessages = 10,
                    )
                }
            }

        // Create sessions with different models to demonstrate isolation and binding
        mobileGraph?.let {
            sessionMap["Session 1"] = it.createSession(modelName = "gpt-4o")
            sessionMap["Session 2"] = it.createSession(modelName = "gpt-4o-mini")
        }

        // Collect events from all sessions to show in UI
        sessionMap.values.forEach { session ->
            viewModelScope.launch {
                session.events.collect { event ->
                    val eventStr =
                        when (event) {
                            is MobileGraphEvent.RequestStarted -> {
                                "Session ${session.sessionId.takeLast(4)}: Started"
                            }

                            is MobileGraphEvent.RequestCompleted -> {
                                "Session ${session.sessionId.takeLast(4)}: Success"
                            }

                            is MobileGraphEvent.RequestFailed -> {
                                "Session ${session.sessionId.takeLast(4)}: Failed - ${event.errorMessage}"
                            }

                            else -> {
                                event.toString()
                            }
                        }
                    withContext(Dispatchers.Main) {
                        eventLog.add(0, eventStr)
                        if (eventLog.size > 10) eventLog.removeAt(eventLog.lastIndex)
                    }
                }
            }
        }

        uiState = "SDK Initialized with 2 Sessions"
    }

    override fun onCleared() {
        super.onCleared()
        // CRITICAL: Frees up memory by closing all active sessions
        sessionMap.values.forEach { it.close() }
        sessionMap.clear()
        Log.d("SampleApp", "Resources cleared.")
    }

    /**
     * Runs a chat invocation using the current session's bound model.
     * Use this pattern for conversational UIs where history matters.
     */
    fun runSessionChat(userInput: String) =
        viewModelScope.launch {
            val session = sessionMap[currentSessionId] ?: return@launch

            executeSample("Session Chat ($currentSessionId)") {
                // Uses the model bound to this specific session (gpt-4o or gpt-4o-mini)
                // History is automatically managed by the session context.
                val output = session.chat(userInput)
                output.asText()
            }
        }

    /**
     * Runs a streaming chat invocation.
     */
    fun runStreamingChat(userInput: String) =
        viewModelScope.launch {
            val session = sessionMap[currentSessionId] ?: return@launch

            isLoading = true
            uiState = "Streaming: $currentSessionId"
            currentOutput = ""

            try {
                // Use the streaming extension for session
                session.stream(userInput).collect { chunk ->
                    withContext(Dispatchers.Main) {
                        currentOutput += chunk.text
                    }
                }
                uiState = "Streaming Success"
            } catch (e: Exception) {
                currentOutput = "Error: ${e.message}"
                uiState = "Streaming Failed"
                Log.e("SampleApp", "Error during streaming", e)
            } finally {
                isLoading = false
            }
        }

    /**
     * Runs a sequence of chat messages in the current session.
     */
    fun runChatSequence() =
        viewModelScope.launch {
            val session = sessionMap[currentSessionId] ?: return@launch
            val sequence =
                listOf(
                    "Hi, I'm testing the MobileGraph SDK. which supports Android and ios",
                    "The sdk supports InMemoryChatMessageHistory, ConversationBufferWindowMemory and ConversationSummaryBufferMemory",
                    "What did I just say in my previous message?",
                    "Tell me a short joke about AI.",
                    "Explain how chat memory works in this SDK concisely.",
                    "The sdk also supports tool function calling, prompt composing, output formatter",
                    "The sdk designed to be model agnostic",
                    "Explain what the MobileGraph sdk based on your chat history",
                )

            isLoading = true
            currentOutput = ""
            uiState = "Running Sequence: $currentSessionId"

            try {
                val results = StringBuilder()
                sequence.forEachIndexed { index, query ->
                    currentSequenceItem = "Query ${index + 1}/${sequence.size}: $query"

                    // Invoke chat for this sequence item
                    val output = session.chat(query)
                    val resultText = output.asText()

                    results.append("Q: $query\nA: $resultText\n\n")
                    currentOutput = results.toString()
                }
                uiState = "Sequence Completed"
            } catch (e: Exception) {
                currentOutput = "Error in sequence: ${e.message}"
                uiState = "Sequence Failed"
                Log.e("SampleApp", "Error running sequence", e)
            } finally {
                isLoading = false
                currentSequenceItem = ""
            }
        }

    /**
     * Runs a stateless utility task using the global default model.
     * Use this pattern for background tasks where history is NOT needed.
     */
    fun runGlobalUtilityTask(userInput: String) =
        viewModelScope.launch {
            executeSample("Global Utility Task") {
                // Accesses the default model directly from the global registry.
                // NO SESSION HISTORY is attached to this call.
                val model = MobileGraph.models.chat()
                val prompt =
                    promptComposer {
                        system("You are a text summarizer. Summarize the user's input.")
                        human(userInput)
                    }.compose()

                val output = model.invoke(prompt)
                output.asText()
            }
        }

    /**
     * Switches the active session.
     */
    fun switchSession(sessionId: String) {
        currentSessionId = sessionId
        uiState = "Switched to $sessionId"
        currentOutput = "Context and Model for $sessionId are isolated."
    }

    /**
     * Clears the global (non-session) chat history.
     */
    fun clearGlobalHistory() =
        viewModelScope.launch {
            mobileGraph?.memory?.clearGlobal()
            uiState = "Global History Cleared"
            currentOutput = "Shared history for non-session tasks has been wiped."
        }

    fun runStructuredParsing(userInput: String) =
        viewModelScope.launch {
            executeSample("Structured Parsing") {
                val session = sessionMap[currentSessionId] ?: return@executeSample "No session"
                val parser = structuredOutputParser<ProductReview>()

                val prompt =
                    promptComposer {
                        system("Analyze the following review.")
                        human(userInput)
                        system(parser.formatInstructions())
                    }.compose()

                // Recommended: use session.model() to keep the context for the parser call
                val output = session.model().invoke(prompt, context = session.internal as ExecutionContext)
                val result = parser.parse(output)

                when (result) {
                    is ParseResult.Success -> "Parsed Review:\nRating: ${result.value.rating}\nSummary: ${result.value.summary}"
                    is ParseResult.Failure -> "Parse Error: ${result.error.message}"
                    is ParseResult.Partial -> "Partial Data: ${result.partialValue}"
                }
            }
        }

    private suspend fun executeSample(
        name: String,
        block: suspend () -> String,
    ) {
        isLoading = true
        uiState = "Running: $name"
        try {
            currentOutput = block()
            uiState = "Success: $name"
        } catch (e: Exception) {
            currentOutput = "Error: ${e.message}"
            uiState = "Failed: $name"
            Log.e("SampleApp", "Error running $name", e)
        } finally {
            isLoading = false
        }
    }

    // Helpers
    private val weatherTool =
        object : Tool<String, String> {
            override val metadata = ToolMetadata("get_weather", "Gets weather for a city")

            override suspend fun invoke(
                input: String,
                context: ExecutionContext,
            ): String = "22°C and Sunny in $input"
        }

    private fun loadCacheFromFile(dir: File): String? = File(dir, "cache.json").let { if (it.exists()) it.readText() else null }

    private fun saveCacheToFile(
        dir: File,
        json: String,
    ) = File(dir, "cache.json").writeText(json)
}

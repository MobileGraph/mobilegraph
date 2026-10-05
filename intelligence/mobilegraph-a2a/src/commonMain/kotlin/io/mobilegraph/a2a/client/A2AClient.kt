package io.mobilegraph.a2a.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.mobilegraph.a2a.errors.A2ADiscoveryException
import io.mobilegraph.a2a.errors.A2ANetworkException
import io.mobilegraph.a2a.errors.A2AProtocolException
import io.mobilegraph.a2a.errors.A2AStreamException
import io.mobilegraph.a2a.errors.A2ATaskNotFoundException
import io.mobilegraph.a2a.errors.A2ATimeoutException
import io.mobilegraph.a2a.errors.A2AValidationException
import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.A2AJsonRpcError
import io.mobilegraph.a2a.models.A2AMessageSendParams
import io.mobilegraph.a2a.models.A2ARequest
import io.mobilegraph.a2a.models.A2AResponse
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskCancelParams
import io.mobilegraph.a2a.models.A2ATaskQueryParams
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.A2ATaskStatus
import io.mobilegraph.a2a.models.AgentCard
import io.mobilegraph.a2a.models.SecurityScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * A2A protocol client for MobileGraph.
 *
 * Enables MobileGraph agents to communicate with remote A2A-compatible agents.
 * Supports agent discovery, message sending (synchronous and streaming),
 * task tracking, and cancellation.
 *
 * ## Architecture
 * ```
 * MobileGraph Agent
 *        │
 *        v
 *    A2AClient
 *        │
 *        │ HTTPS / JSON-RPC 2.0
 *        v
 * Remote A2A Agent
 * ```
 *
 * ## Usage
 * ```kotlin
 * val client = A2AClient(httpClient)
 *
 * // Discover a remote agent
 * val agentCard = client.discoverAgent("https://remote-agent.example.com")
 *
 * // Send a message
 * val task = client.sendMessage(agentCard.url, params)
 *
 * // Stream updates
 * client.streamMessage(agentCard.url, params).collect { event ->
 *     when (event) {
 *         is A2AStreamEvent.TaskStatusUpdate -> { ... }
 *         is A2AStreamEvent.TaskArtifactUpdate -> { ... }
 *         is A2AStreamEvent.TaskComplete -> { ... }
 *         is A2AStreamEvent.StreamError -> { ... }
 *     }
 * }
 * ```
 *
 * @property httpClient The Ktor HTTP client for network communication.
 * @property config Client configuration (timeouts, retries, payload limits).
 * @property authProvider Optional authentication provider.
 */
class A2AClient(
    private val httpClient: HttpClient,
    private val config: A2AClientConfig = A2AClientConfig(),
    private val authProvider: A2AAuthProvider = NoAuthProvider(),
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
            isLenient = true
        }

    private val responseParser = A2AResponseParser(json)
    private val streamProcessor = A2AStreamProcessor(json, responseParser)

    private var nextId = 1
    private val idMutex = Mutex()

    // Cache discovered agent cards to avoid re-fetching
    private val agentCardCache = mutableMapOf<String, AgentCard>()
    private val cacheMutex = Mutex()

    /**
     * Discovers a remote A2A agent by fetching its Agent Card.
     *
     * Fetches `/.well-known/agent-card.json` from the agent's base URL.
     *
     * @param baseUrl The base URL of the remote agent (e.g., "https://agent.example.com").
     * @param forceRefresh If true, bypasses the cache and re-fetches.
     * @return The parsed [AgentCard].
     * @throws A2ADiscoveryException if the agent card cannot be fetched or parsed.
     */
    suspend fun discoverAgent(
        baseUrl: String,
        forceRefresh: Boolean = false,
    ): AgentCard {
        // Check cache first
        if (!forceRefresh) {
            cacheMutex.withLock {
                agentCardCache[baseUrl]?.let { return it }
            }
        }

        val agentCardUrl = buildAgentCardUrl(baseUrl)

        try {
            val response =
                httpClient.get(agentCardUrl) {
                    header("Accept", "application/json")
                }

            if (response.status.value !in 200..299) {
                throw A2ADiscoveryException(
                    "Failed to fetch Agent Card from $agentCardUrl: HTTP ${response.status.value}",
                )
            }

            val responseText = response.bodyAsText()

            val agentCard =
                try {
                    json.decodeFromString<AgentCard>(responseText)
                } catch (e: Exception) {
                    throw A2ADiscoveryException(
                        "Failed to parse Agent Card from $agentCardUrl: ${e.message}",
                        e,
                    )
                }

            // Validate required fields
            if (agentCard.name.isBlank()) {
                throw A2AValidationException("Agent Card missing required 'name' field")
            }
            if (agentCard.url.isBlank()) {
                throw A2AValidationException("Agent Card missing required 'url' field")
            }

            // Cache the result
            cacheMutex.withLock {
                agentCardCache[baseUrl] = agentCard
            }

            return agentCard
        } catch (e: A2ADiscoveryException) {
            throw e
        } catch (e: A2AValidationException) {
            throw e
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw A2ATimeoutException("Timed out discovering agent at $agentCardUrl", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw A2ADiscoveryException(
                "Failed to discover agent at $agentCardUrl: ${e.message}",
                e,
            )
        }
    }

    /**
     * Sends a message to a remote A2A agent (synchronous request/response).
     *
     * Uses A2A v1.0 `SendMessage` method by default, with automatic fallback
     * to legacy `message/send` (A2A v0.3) on `-32601` (Method not found) errors.
     *
     * @param agentUrl The A2A JSON-RPC endpoint URL.
     * @param params The message send parameters.
     * @param securitySchemes Security schemes from the agent card for authentication.
     * @return The resulting [A2ATask].
     * @throws A2AProtocolException if the remote agent returns an error.
     * @throws A2ANetworkException if a network error occurs.
     * @throws A2ATimeoutException if the request times out.
     */
    suspend fun sendMessage(
        agentUrl: String,
        params: A2AMessageSendParams,
        securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    ): A2ATask {
        val jsonParams = json.encodeToJsonElement(params).jsonObject
        val primaryMethod = A2AMethods.resolve(A2AMethods.SEND_MESSAGE, config.useLegacyMethodNames)

        val response =
            try {
                call(primaryMethod, agentUrl, jsonParams, securitySchemes)
            } catch (e: A2AProtocolException) {
                if (e.errorCode == A2AJsonRpcError.METHOD_NOT_FOUND &&
                    config.autoFallbackToLegacy &&
                    !config.useLegacyMethodNames
                ) {
                    val legacyMethod = A2AMethods.toLegacy(A2AMethods.SEND_MESSAGE)
                    call(legacyMethod, agentUrl, jsonParams, securitySchemes)
                } else {
                    throw e
                }
            }
        return responseParser.parseTaskResponse(response)
    }

    /**
     * Streams a message to a remote A2A agent via SSE.
     *
     * Uses A2A v1.0 `SendStreamingMessage` method by default, with automatic fallback
     * to `StreamMessage` and legacy `message/stream` (A2A v0.3) if the server returns `-32601`.
     *
     * @param agentUrl The A2A JSON-RPC endpoint URL.
     * @param params The message send parameters.
     * @param securitySchemes Security schemes from the agent card for authentication.
     * @return A [Flow] of [A2AStreamEvent].
     */
    fun streamMessage(
        agentUrl: String,
        params: A2AMessageSendParams,
        securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    ): Flow<A2AStreamEvent> =
        flow {
            val methodsToTry =
                if (config.useLegacyMethodNames) {
                    listOf(A2AMethods.LEGACY_STREAM_MESSAGE)
                } else if (config.autoFallbackToLegacy) {
                    listOf(
                        A2AMethods.SEND_STREAMING_MESSAGE,
                        A2AMethods.STREAM_MESSAGE,
                        A2AMethods.LEGACY_STREAM_MESSAGE,
                    )
                } else {
                    listOf(A2AMethods.SEND_STREAMING_MESSAGE)
                }

            for ((index, method) in methodsToTry.withIndex()) {
                var methodNotFound = false
                var hasEmittedEvents = false

                tryStreamMessage(method, agentUrl, params, securitySchemes).collect { event ->
                    if (event is A2AStreamEvent.StreamError &&
                        event.error.code == A2AJsonRpcError.METHOD_NOT_FOUND &&
                        index < methodsToTry.lastIndex
                    ) {
                        methodNotFound = true
                    } else {
                        hasEmittedEvents = true
                        emit(event)
                    }
                }

                if (!methodNotFound || hasEmittedEvents) {
                    break
                }
            }
        }

    private fun tryStreamMessage(
        method: String,
        agentUrl: String,
        params: A2AMessageSendParams,
        securitySchemes: Map<String, SecurityScheme>,
    ): Flow<A2AStreamEvent> =
        flow {
            val id = idMutex.withLock { nextId++ }
            val jsonParams = json.encodeToJsonElement(params).jsonObject

            val request =
                A2ARequest(
                    id = JsonPrimitive(id),
                    method = method,
                    params = jsonParams,
                )
            val requestBody = json.encodeToString(request)

            try {
                val statement =
                    httpClient.preparePost(agentUrl) {
                        header("Accept", "text/event-stream")
                        contentType(ContentType.Application.Json)
                        setBody(requestBody)
                        authProvider.authenticate(this, securitySchemes)
                    }

                statement.execute { response ->
                    if (response.status.value !in 200..299) {
                        emit(
                            A2AStreamEvent.StreamError(
                                A2AJsonRpcError(
                                    code = A2AJsonRpcError.INTERNAL_ERROR,
                                    message = "Stream request failed: HTTP ${response.status.value}",
                                ),
                            ),
                        )
                        return@execute
                    }

                    val channel = response.body<ByteReadChannel>()
                    streamProcessor.processStreamEvents(channel, this)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(
                    A2AStreamEvent.StreamError(
                        A2AJsonRpcError(
                            code = A2AJsonRpcError.INTERNAL_ERROR,
                            message = "Stream error: ${e.message}",
                        ),
                    ),
                )
            }
        }

    /**
     * Retrieves the current status and details of a task.
     *
     * Uses A2A v1.0 `GetTask` method by default, with automatic fallback
     * to legacy `tasks/get` (A2A v0.3) on `-32601` (Method not found).
     *
     * @param agentUrl The A2A JSON-RPC endpoint URL.
     * @param params The task query parameters.
     * @param securitySchemes Security schemes from the agent card for authentication.
     * @return The current [A2ATask].
     * @throws A2ATaskNotFoundException if the task does not exist.
     * @throws A2AProtocolException if the remote agent returns an error.
     */
    suspend fun getTask(
        agentUrl: String,
        params: A2ATaskQueryParams,
        securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    ): A2ATask {
        val jsonParams = json.encodeToJsonElement(params).jsonObject
        val primaryMethod = A2AMethods.resolve(A2AMethods.GET_TASK, config.useLegacyMethodNames)

        val response =
            try {
                call(primaryMethod, agentUrl, jsonParams, securitySchemes)
            } catch (e: A2AProtocolException) {
                if (e.errorCode == A2AJsonRpcError.METHOD_NOT_FOUND &&
                    config.autoFallbackToLegacy &&
                    !config.useLegacyMethodNames
                ) {
                    val legacyMethod = A2AMethods.toLegacy(A2AMethods.GET_TASK)
                    call(legacyMethod, agentUrl, jsonParams, securitySchemes)
                } else {
                    throw e
                }
            }
        return responseParser.parseTaskResponse(response)
    }

    suspend fun cancelTask(
        agentUrl: String,
        params: A2ATaskCancelParams,
        securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    ): A2ATask {
        val jsonParams = json.encodeToJsonElement(params).jsonObject
        val primaryMethod = A2AMethods.resolve(A2AMethods.CANCEL_TASK, config.useLegacyMethodNames)

        val response =
            try {
                call(primaryMethod, agentUrl, jsonParams, securitySchemes)
            } catch (e: A2AProtocolException) {
                if (e.errorCode == A2AJsonRpcError.METHOD_NOT_FOUND &&
                    config.autoFallbackToLegacy &&
                    !config.useLegacyMethodNames
                ) {
                    val legacyMethod = A2AMethods.toLegacy(A2AMethods.CANCEL_TASK)
                    call(legacyMethod, agentUrl, jsonParams, securitySchemes)
                } else {
                    throw e
                }
            }
        return responseParser.parseTaskResponse(response)
    }

    /**
     * Polls a task until it reaches a terminal state.
     *
     * Periodically calls `tasks/get` until the task is completed, failed,
     * or canceled. Useful when the remote agent does not support streaming.
     *
     * @param agentUrl The A2A JSON-RPC endpoint URL.
     * @param taskId The task ID to poll.
     * @param securitySchemes Security schemes from the agent card for authentication.
     * @param maxAttempts Maximum number of polling attempts (0 = unlimited).
     * @return A [Flow] of [A2ATask] representing status updates.
     */
    fun pollTask(
        agentUrl: String,
        taskId: String,
        securitySchemes: Map<String, SecurityScheme> = emptyMap(),
        maxAttempts: Int = 0,
    ): Flow<A2ATask> =
        flow {
            var attempts = 0
            while (maxAttempts == 0 || attempts < maxAttempts) {
                attempts++
                val task =
                    getTask(
                        agentUrl,
                        A2ATaskQueryParams(id = taskId),
                        securitySchemes,
                    )
                emit(task)

                if (task.status.state.isTerminal()) {
                    break
                }

                delay(config.taskPollingIntervalMs)
            }
        }

    /**
     * Clears the agent card cache.
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            agentCardCache.clear()
        }
    }

    /**
     * Closes the client and releases resources.
     */
    fun close() {
        httpClient.close()
    }

    // --- Private Helpers ---

    /**
     * Sends a JSON-RPC request and returns the response.
     */
    private suspend fun call(
        method: String,
        agentUrl: String,
        params: kotlinx.serialization.json.JsonObject,
        securitySchemes: Map<String, SecurityScheme>,
    ): A2AResponse {
        val id = idMutex.withLock { nextId++ }
        val request =
            A2ARequest(
                id = JsonPrimitive(id),
                method = method,
                params = params,
            )
        val requestBody = json.encodeToString(request)

        return executeWithRetry(method) {
            try {
                val response =
                    httpClient.post(agentUrl) {
                        contentType(ContentType.Application.Json)
                        header("Accept", "application/json")
                        setBody(requestBody)
                        authProvider.authenticate(this, securitySchemes)
                    }

                if (response.status.value !in 200..299) {
                    throw A2ANetworkException(
                        "A2A request $method failed: HTTP ${response.status.value}",
                    )
                }

                val responseText = response.bodyAsText()

                val a2aResponse =
                    try {
                        json.decodeFromString<A2AResponse>(responseText)
                    } catch (e: Exception) {
                        throw A2AProtocolException(
                            "Failed to parse A2A response for $method: ${e.message}",
                            cause = e,
                        )
                    }

                // Check for JSON-RPC error
                a2aResponse.error?.let { error ->
                    when (error.code) {
                        A2AJsonRpcError.TASK_NOT_FOUND -> {
                            throw A2ATaskNotFoundException(
                                params["id"]?.toString()?.trim('"') ?: "unknown",
                            )
                        }

                        else -> {
                            throw A2AProtocolException(
                                "A2A $method error: ${error.message}",
                                error.code,
                            )
                        }
                    }
                }

                a2aResponse
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw A2ATimeoutException("A2A request $method timed out after ${config.requestTimeoutMs}ms", e)
            }
        }
    }

    /**
     * Executes an operation with retry logic for idempotent methods.
     *
     * Only retries on network errors. Does not retry on protocol errors,
     * authentication failures, or task-not-found errors.
     *
     * Non-idempotent operations (message/send) are not retried to avoid
     * duplicate task creation.
     */
    private suspend fun <T> executeWithRetry(
        method: String,
        operation: suspend () -> T,
    ): T {
        // Only retry idempotent operations
        val isIdempotent =
            method in
                setOf(
                    A2AMethods.GET_TASK,
                    A2AMethods.CANCEL_TASK,
                    A2AMethods.LEGACY_GET_TASK,
                    A2AMethods.LEGACY_CANCEL_TASK,
                )
        val maxRetries = if (isIdempotent) config.maxRetries else 0

        var lastException: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return operation()
            } catch (e: A2ANetworkException) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = config.retryDelayMs * (1L shl attempt) // Exponential backoff
                    delay(delayMs)
                }
            } catch (e: A2ATimeoutException) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = config.retryDelayMs * (1L shl attempt)
                    delay(delayMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Don't retry protocol errors, auth errors, etc.
                throw e
            }
        }
        throw lastException ?: A2ANetworkException("A2A request $method failed after retries")
    }

    /**
     * Builds the Agent Card discovery URL from a base URL.
     */
    private fun buildAgentCardUrl(baseUrl: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        return "$cleanBase/.well-known/agent-card.json"
    }

    companion object {
        /** A2A protocol version supported by this client (v1.0 compliant). */
        const val PROTOCOL_VERSION = "1.0.0"
    }
}

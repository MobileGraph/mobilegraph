package io.mobilegraph.a2a.node

import io.mobilegraph.a2a.adapter.A2ATaskAdapter
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.client.A2AStreamEvent
import io.mobilegraph.a2a.errors.A2AException
import io.mobilegraph.a2a.models.A2AMessage
import io.mobilegraph.a2a.models.A2AMessageSendConfiguration
import io.mobilegraph.a2a.models.A2AMessageSendParams
import io.mobilegraph.a2a.models.A2ARole
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskQueryParams
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.SecurityScheme
import io.mobilegraph.a2a.models.TextPart
import io.mobilegraph.core.events.EventPublisher
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.GraphNode
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.delay

/**
 * A graph node that delegates work to a remote A2A agent.
 *
 * This node integrates seamlessly into MobileGraph graphs like any other node.
 * When executed, it sends a message to a remote A2A agent, tracks the task,
 * and maps the result back into the graph state.
 *
 * ## Architecture
 * ```
 * Previous Graph Node
 *        │
 *        v
 *    A2ANode
 *        │
 *        │ HTTPS / JSON-RPC 2.0
 *        v
 * Remote A2A Agent
 *        │
 *        │ Response / Streaming
 *        v
 *    A2ANode
 *        │
 *        │ Maps result to GraphState
 *        v
 * Next Graph Node
 * ```
 *
 * ## Features
 * - **Synchronous mode**: Sends message, polls for completion
 * - **Streaming mode**: Uses SSE for real-time updates
 * - **Input-required**: Maps to `ExecutionResult.AwaitingReview` for HITL
 * - **Error handling**: Maps failures to `ExecutionResult.Error`
 * - **Observability**: Publishes `MobileGraphEvent` for all state transitions
 *
 * @param id Unique node identifier within the graph.
 * @param client The A2A client to use for communication.
 * @param agentUrl The A2A JSON-RPC endpoint URL of the remote agent.
 * @param messageBuilder Builds the A2A message from current graph state.
 * @param resultMapper Optional custom mapper for task results (default uses [A2ATaskAdapter]).
 * @param useStreaming Whether to use SSE streaming (requires remote agent support).
 * @param securitySchemes Security schemes for authentication.
 * @param config Node-specific configuration.
 */
class A2ANode(
    override val id: String,
    private val client: A2AClient,
    private val agentUrl: String,
    private val messageBuilder: (GraphState) -> A2AMessage,
    private val resultMapper: ((A2ATask, GraphState) -> GraphState)? = null,
    private val useStreaming: Boolean = false,
    private val securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    private val config: A2ANodeConfig = A2ANodeConfig(),
) : GraphNode {
    override suspend fun execute(state: GraphState): ExecutionResult {
        val eventPublisher =
            try {
                MobileGraph.instance.getComponent(EventPublisher::class)
            } catch (_: Exception) {
                null
            }

        val context = state.executionContext

        try {
            // Build the message from current state
            val rawMessage = messageBuilder(state)

            // Auto-attach HITL task context (taskId, contextId, messageId) if resuming from input-required state
            val previousTaskId = state.variables[A2ATaskAdapter.STATE_KEY_A2A_TASK_ID] as? String
            val previousContextId = state.variables[A2ATaskAdapter.STATE_KEY_A2A_CONTEXT_ID] as? String

            val message =
                if (!previousTaskId.isNullOrBlank()) {
                    rawMessage.copy(
                        taskId = rawMessage.taskId ?: previousTaskId,
                        contextId = rawMessage.contextId ?: previousContextId,
                        messageId = rawMessage.messageId ?: "msg-hitl-${kotlin.random.Random.nextInt(1000, 9999)}",
                    )
                } else {
                    rawMessage
                }

            // Publish task submitted event
            eventPublisher?.publish(
                MobileGraphEvent.A2ATaskSubmitted(
                    traceId = context.traceId,
                    requestId = context.requestId,
                    remoteAgent = agentUrl,
                    nodeId = id,
                ),
            )

            val task: A2ATask =
                if (useStreaming) {
                    try {
                        executeStreaming(message, eventPublisher, state)
                    } catch (_: io.mobilegraph.a2a.errors.A2AStreamException) {
                        // If remote agent does not support StreamMessage / streaming, fallback to synchronous SendMessage
                        executeSynchronous(message, eventPublisher, state)
                    }
                } else {
                    executeSynchronous(message, eventPublisher, state)
                }

            // Validate the response (trust boundary)
            A2ATaskAdapter.validateTask(task)

            // Handle input-required → map to AwaitingReview
            if (A2ATaskAdapter.isInputRequired(task)) {
                val updatedState = A2ATaskAdapter.mapTaskToState(task, state, agentUrl)
                eventPublisher?.publish(
                    MobileGraphEvent.A2ATaskStatusChanged(
                        traceId = context.traceId,
                        requestId = context.requestId,
                        taskId = task.id,
                        status = "input-required",
                        remoteAgent = agentUrl,
                        nodeId = id,
                    ),
                )
                return ExecutionResult.AwaitingReview(
                    nodeId = id,
                    state = updatedState,
                )
            }

            // Handle failure
            if (task.status.state == A2ATaskState.FAILED) {
                val errorState = A2ATaskAdapter.mapTaskToState(task, state, agentUrl)
                eventPublisher?.publish(
                    MobileGraphEvent.A2ATaskFailed(
                        traceId = context.traceId,
                        requestId = context.requestId,
                        taskId = task.id,
                        error =
                            task.status.message
                                ?.parts
                                ?.filterIsInstance<TextPart>()
                                ?.joinToString(" ") { it.text }
                                ?: "Remote task failed",
                        remoteAgent = agentUrl,
                        nodeId = id,
                    ),
                )
                return ExecutionResult.Error(errorState)
            }

            // Handle success — map result to state
            val resultState =
                if (resultMapper != null) {
                    resultMapper.invoke(task, state)
                } else {
                    A2ATaskAdapter.mapTaskToState(task, state, agentUrl)
                }

            eventPublisher?.publish(
                MobileGraphEvent.A2ATaskCompleted(
                    traceId = context.traceId,
                    requestId = context.requestId,
                    taskId = task.id,
                    remoteAgent = agentUrl,
                    nodeId = id,
                ),
            )

            return ExecutionResult.Success(resultState)
        } catch (e: A2AException) {
            eventPublisher?.publish(
                MobileGraphEvent.A2ATaskFailed(
                    traceId = context.traceId,
                    requestId = context.requestId,
                    taskId = "",
                    error = e.message ?: "A2A error",
                    remoteAgent = agentUrl,
                    nodeId = id,
                ),
            )
            return ExecutionResult.Error(state)
        } catch (e: Exception) {
            eventPublisher?.publish(
                MobileGraphEvent.A2ATaskFailed(
                    traceId = context.traceId,
                    requestId = context.requestId,
                    taskId = "",
                    error = e.message ?: "Unknown error",
                    remoteAgent = agentUrl,
                    nodeId = id,
                ),
            )
            return ExecutionResult.Error(state)
        }
    }

    /**
     * Sends a message synchronously and polls until completion.
     */
    private suspend fun executeSynchronous(
        message: A2AMessage,
        eventPublisher: EventPublisher?,
        state: GraphState,
    ): A2ATask {
        val params =
            A2AMessageSendParams(
                message = message,
                configuration =
                    A2AMessageSendConfiguration(
                        historyLength = config.historyLength,
                        acceptedOutputModes = config.acceptedOutputModes,
                    ),
            )

        var task = client.sendMessage(agentUrl, params, securitySchemes)

        // If task is not yet terminal, poll for completion
        if (!task.status.state.isTerminal() && task.status.state != A2ATaskState.INPUT_REQUIRED) {
            var pollAttempts = 0
            while (!task.status.state.isTerminal() &&
                task.status.state != A2ATaskState.INPUT_REQUIRED
            ) {
                if (config.maxPollAttempts > 0 && pollAttempts >= config.maxPollAttempts) {
                    break
                }
                pollAttempts++

                delay(config.pollIntervalMs)

                task =
                    client.getTask(
                        agentUrl,
                        A2ATaskQueryParams(id = task.id),
                        securitySchemes,
                    )

                // Publish status update event
                eventPublisher?.publish(
                    MobileGraphEvent.A2ATaskStatusChanged(
                        traceId = state.executionContext.traceId,
                        requestId = state.executionContext.requestId,
                        taskId = task.id,
                        status = task.status.state.name,
                        remoteAgent = agentUrl,
                        nodeId = id,
                    ),
                )
            }
        }

        return task
    }

    /**
     * Sends a message with SSE streaming and collects events.
     */
    private suspend fun executeStreaming(
        message: A2AMessage,
        eventPublisher: EventPublisher?,
        state: GraphState,
    ): A2ATask {
        val params =
            A2AMessageSendParams(
                message = message,
                configuration =
                    A2AMessageSendConfiguration(
                        historyLength = config.historyLength,
                        acceptedOutputModes = config.acceptedOutputModes,
                    ),
            )

        var latestTask: A2ATask? = null
        var lastTaskId = ""
        var lastStatus: io.mobilegraph.a2a.models.A2ATaskStatus? = null
        val accumulatedArtifacts = mutableListOf<io.mobilegraph.a2a.models.A2AArtifact>()

        client
            .streamMessage(agentUrl, params, securitySchemes)
            .collect { event ->
                when (event) {
                    is A2AStreamEvent.TaskStatusUpdate -> {
                        lastTaskId = event.taskId
                        lastStatus = event.status
                        eventPublisher?.publish(
                            MobileGraphEvent.A2ATaskStatusChanged(
                                traceId = state.executionContext.traceId,
                                requestId = state.executionContext.requestId,
                                taskId = event.taskId,
                                status = event.status.state.name,
                                remoteAgent = agentUrl,
                                nodeId = id,
                            ),
                        )
                    }

                    is A2AStreamEvent.TaskArtifactUpdate -> {
                        lastTaskId = event.taskId
                        accumulatedArtifacts.add(event.artifact)
                        eventPublisher?.publish(
                            MobileGraphEvent.A2AArtifactReceived(
                                traceId = state.executionContext.traceId,
                                requestId = state.executionContext.requestId,
                                taskId = event.taskId,
                                artifactId = event.artifact.artifactId ?: "",
                                remoteAgent = agentUrl,
                                nodeId = id,
                            ),
                        )
                    }

                    is A2AStreamEvent.TaskComplete -> {
                        latestTask = event.task
                    }

                    is A2AStreamEvent.StreamError -> {
                        throw io.mobilegraph.a2a.errors.A2AStreamException(
                            "Stream error: ${event.error.message}",
                        )
                    }
                }
            }

        if (latestTask != null) {
            val task = latestTask!!
            // Merge accumulated artifacts/messages if task.artifacts is empty
            val mergedArtifacts =
                if (task.artifacts.isEmpty() && accumulatedArtifacts.isNotEmpty()) {
                    accumulatedArtifacts
                } else {
                    task.artifacts
                }

            val mergedStatus =
                if (task.status.message == null && lastStatus?.message != null) {
                    task.status.copy(message = lastStatus.message)
                } else {
                    task.status
                }

            return task.copy(
                artifacts = mergedArtifacts,
                status = mergedStatus,
            )
        }

        // Fallback: Construct final task from stream events if not sent as a full TaskComplete event
        if (lastTaskId.isNotBlank() || accumulatedArtifacts.isNotEmpty() || lastStatus != null) {
            val defaultStatus =
                io.mobilegraph.a2a.models
                    .A2ATaskStatus(state = io.mobilegraph.a2a.models.A2ATaskState.COMPLETED)
            return A2ATask(
                id = lastTaskId.ifBlank { "task-stream-${kotlin.random.Random.nextInt(1000, 9999)}" },
                status = lastStatus ?: defaultStatus,
                artifacts = accumulatedArtifacts,
            )
        }

        // Seamless fallback to synchronous messaging if SSE stream produces no task result
        return executeSynchronous(message, eventPublisher, state)
    }
}

/**
 * Configuration for an [A2ANode].
 *
 * @property historyLength Number of history messages to include in requests.
 * @property acceptedOutputModes Accepted output content types.
 * @property pollIntervalMs Interval between task status polls (sync mode).
 * @property maxPollAttempts Maximum poll attempts (0 = unlimited).
 */
data class A2ANodeConfig(
    val historyLength: Int? = null,
    val acceptedOutputModes: List<String>? = null,
    val pollIntervalMs: Long = 2_000L,
    val maxPollAttempts: Int = 300, // 10 minutes at 2s interval
)

/**
 * DSL builder for creating an [A2ANode] with a convenient syntax.
 *
 * ## Usage
 * ```kotlin
 * val node = a2aNode("analyze") {
 *     client = a2aClient
 *     agentUrl = "https://coding-agent.example.com"
 *     message { state ->
 *         A2AMessage(
 *             role = A2ARole.USER,
 *             parts = listOf(TextPart(text = state.variables["query"] as String))
 *         )
 *     }
 *     streaming = true
 * }
 * ```
 */
fun a2aNode(
    id: String,
    block: A2ANodeBuilder.() -> Unit,
): A2ANode {
    val builder = A2ANodeBuilder(id)
    builder.block()
    return builder.build()
}

/**
 * Builder for [A2ANode].
 */
class A2ANodeBuilder(
    private val id: String,
) {
    /** The A2A client to use. */
    var client: A2AClient? = null

    /** The remote agent's A2A endpoint URL. */
    var agentUrl: String? = null

    /** Whether to use SSE streaming. */
    var streaming: Boolean = false

    /** Security schemes for authentication. */
    var securitySchemes: Map<String, SecurityScheme> = emptyMap()

    /** Node configuration. */
    var config: A2ANodeConfig = A2ANodeConfig()

    private var messageBuilder: ((GraphState) -> A2AMessage)? = null
    private var resultMapper: ((A2ATask, GraphState) -> GraphState)? = null

    /**
     * Sets the message builder function.
     */
    fun message(builder: (GraphState) -> A2AMessage) {
        messageBuilder = builder
    }

    /**
     * Sets a simple text message builder.
     */
    fun textMessage(textBuilder: (GraphState) -> String) {
        messageBuilder = { state ->
            A2AMessage(
                role = A2ARole.USER,
                parts = listOf(TextPart(text = textBuilder(state))),
            )
        }
    }

    /**
     * Sets a custom result mapper.
     */
    fun mapResult(mapper: (A2ATask, GraphState) -> GraphState) {
        resultMapper = mapper
    }

    fun build(): A2ANode {
        requireNotNull(client) { "A2ANode requires a client" }
        requireNotNull(agentUrl) { "A2ANode requires an agentUrl" }
        requireNotNull(messageBuilder) { "A2ANode requires a message builder" }

        return A2ANode(
            id = id,
            client = client!!,
            agentUrl = agentUrl!!,
            messageBuilder = messageBuilder!!,
            resultMapper = resultMapper,
            useStreaming = streaming,
            securitySchemes = securitySchemes,
            config = config,
        )
    }
}

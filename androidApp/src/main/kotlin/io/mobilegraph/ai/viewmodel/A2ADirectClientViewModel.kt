package io.mobilegraph.ai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.mobilegraph.a2a.adapter.A2AAgentDescriptor
import io.mobilegraph.a2a.adapter.A2AArtifactAdapter
import io.mobilegraph.a2a.client.A2AAuthProvider
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.client.A2AClientConfig
import io.mobilegraph.a2a.client.A2AStreamEvent
import io.mobilegraph.a2a.client.BearerTokenAuthProvider
import io.mobilegraph.a2a.client.NoAuthProvider
import io.mobilegraph.a2a.models.A2AMessage
import io.mobilegraph.a2a.models.A2AMessageSendParams
import io.mobilegraph.a2a.models.A2ARole
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.TextPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel demonstrating direct client-to-server A2A (Agent-to-Agent) communication.
 *
 * This sample shows how to connect to an A2A server directly using [A2AClient]
 * without requiring graph-based workflows or state machine orchestration.
 *
 * Features demonstrated:
 * 1. Discovering remote A2A Agent Cards (`/.well-known/agent-card.json`)
 * 2. Sending direct synchronous tasks (`SendMessage`)
 * 3. Streaming live updates over SSE (`SendStreamingMessage`)
 * 4. Handling A2A Human-in-the-Loop (`INPUT_REQUIRED`) with follow-up responses (`taskId`, `contextId`)
 */
class A2ADirectClientViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var agentDescriptor by mutableStateOf<A2AAgentDescriptor?>(null)
    var taskResult by mutableStateOf("")
    var remoteAgentUrl by mutableStateOf("https://a2a-test-server-679161232357.us-central1.run.app")
    var authToken by mutableStateOf("")
    var pendingHitlTask by mutableStateOf<A2ATask?>(null)

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private fun createA2AClient(): A2AClient {
        val authProvider: A2AAuthProvider =
            if (authToken.isNotBlank()) {
                BearerTokenAuthProvider(authToken)
            } else {
                NoAuthProvider()
            }

        return A2AClient(
            httpClient = HttpClient(),
            config =
                A2AClientConfig(
                    requestTimeoutMs = 120_000L,
                    socketTimeoutMs = 120_000L,
                ),
            authProvider = authProvider,
        )
    }

    /**
     * Discovers the remote A2A agent by fetching its Agent Card.
     */
    fun discoverAgent() {
        viewModelScope.launch {
            isLoading = true
            uiState = "Discovering Agent Card from $remoteAgentUrl..."
            addLog("Fetching Agent Card from $remoteAgentUrl/.well-known/agent-card.json")

            val client = createA2AClient()
            try {
                val card = client.discoverAgent(remoteAgentUrl, forceRefresh = true)
                agentDescriptor = A2AAgentDescriptor.from(card)
                uiState = "Agent Discovered: ${card.name} (A2A v${card.protocolVersion ?: "1.0"})"
                addLog("Discovered Agent: '${card.name}' with ${card.skills.size} skills")
            } catch (e: Exception) {
                uiState = "Discovery Error: ${e.message}"
                addLog("Discovery Error: ${e.message}")
            } finally {
                client.close()
                isLoading = false
            }
        }
    }

    /**
     * Sends a direct task to the A2A server in synchronous mode (`SendMessage`).
     */
    fun sendDirectTask(prompt: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Sending task to A2A server..."
            taskResult = ""
            pendingHitlTask = null
            addLog("Sending Direct Task → '$prompt'")

            val client = createA2AClient()
            try {
                val card =
                    agentDescriptor?.let {
                        io.mobilegraph.a2a.models.AgentCard(
                            name = it.name,
                            url = it.url,
                            description = it.description,
                        )
                    } ?: client.discoverAgent(remoteAgentUrl)

                val task =
                    client.sendMessage(
                        agentUrl = card.url,
                        params =
                            A2AMessageSendParams(
                                message =
                                    A2AMessage(
                                        role = A2ARole.USER,
                                        parts = listOf(TextPart(text = prompt)),
                                    ),
                            ),
                    )

                handleTaskResponse(task)
            } catch (e: Exception) {
                uiState = "Request Error: ${e.message}"
                addLog("Request Exception: ${e.message}")
            } finally {
                client.close()
                isLoading = false
            }
        }
    }

    /**
     * Sends a streaming request over SSE (`SendStreamingMessage`).
     */
    fun streamDirectTask(prompt: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Streaming task via SSE..."
            taskResult = ""
            pendingHitlTask = null
            addLog("Starting SSE Stream → '$prompt'")

            val client = createA2AClient()
            try {
                val card =
                    agentDescriptor?.let {
                        io.mobilegraph.a2a.models.AgentCard(
                            name = it.name,
                            url = it.url,
                            description = it.description,
                        )
                    } ?: client.discoverAgent(remoteAgentUrl)

                client
                    .streamMessage(
                        agentUrl = card.url,
                        params =
                            A2AMessageSendParams(
                                message =
                                    A2AMessage(
                                        role = A2ARole.USER,
                                        parts = listOf(TextPart(text = prompt)),
                                    ),
                            ),
                    ).collect { event ->
                        when (event) {
                            is A2AStreamEvent.TaskStatusUpdate -> {
                                addLog("Stream Update → Task: ${event.taskId}, Status: ${event.status.state}")
                            }

                            is A2AStreamEvent.TaskArtifactUpdate -> {
                                val chunk = A2AArtifactAdapter.extractText(event.artifact)
                                if (chunk.isNotBlank()) {
                                    taskResult = if (taskResult.isBlank()) chunk else "$taskResult\n$chunk"
                                }
                                addLog("Stream Chunk Received → ${event.artifact.name ?: "Artifact"}")
                            }

                            is A2AStreamEvent.TaskComplete -> {
                                uiState = "Stream Completed"
                                handleTaskResponse(event.task)
                            }

                            is A2AStreamEvent.StreamError -> {
                                uiState = "Stream Error: ${event.error.message}"
                                addLog("Stream Error: ${event.error.message}")
                            }
                        }
                    }
            } catch (e: Exception) {
                uiState = "Stream Exception: ${e.message}"
                addLog("Stream Exception: ${e.message}")
            } finally {
                client.close()
                isLoading = false
            }
        }
    }

    /**
     * Responds to an A2A Human-in-the-Loop (`INPUT_REQUIRED`) task request.
     * Preserves the original `taskId` and `contextId` so the remote agent correlates the feedback.
     */
    fun submitHitlFeedback(feedback: String) {
        val hitlTask = pendingHitlTask ?: return
        viewModelScope.launch {
            isLoading = true
            uiState = "Sending HITL Feedback ('$feedback')..."
            addLog("Submitting HITL Feedback for Task ${hitlTask.id} → '$feedback'")

            val client = createA2AClient()
            try {
                val card =
                    agentDescriptor?.let {
                        io.mobilegraph.a2a.models.AgentCard(
                            name = it.name,
                            url = it.url,
                            description = it.description,
                        )
                    } ?: client.discoverAgent(remoteAgentUrl)

                val completedTask =
                    client.sendMessage(
                        agentUrl = card.url,
                        params =
                            A2AMessageSendParams(
                                message =
                                    A2AMessage(
                                        role = A2ARole.USER,
                                        parts = listOf(TextPart(text = feedback)),
                                        taskId = hitlTask.id,
                                        contextId = hitlTask.contextId,
                                    ),
                            ),
                    )

                pendingHitlTask = null
                handleTaskResponse(completedTask)
            } catch (e: Exception) {
                uiState = "HITL Feedback Error: ${e.message}"
                addLog("HITL Exception: ${e.message}")
            } finally {
                client.close()
                isLoading = false
            }
        }
    }

    private fun handleTaskResponse(task: A2ATask) {
        when (task.status.state) {
            A2ATaskState.COMPLETED -> {
                uiState = "Task Completed"
                pendingHitlTask = null
                val resultText =
                    A2AArtifactAdapter
                        .extractAllText(task.artifacts)
                        .ifBlank {
                            task.status.message
                                ?.parts
                                ?.filterIsInstance<TextPart>()
                                ?.joinToString("\n") { it.text }
                                ?: "Task completed successfully"
                        }
                taskResult = resultText
                addLog("Task ${task.id} Completed → Result: $resultText")
            }

            A2ATaskState.INPUT_REQUIRED -> {
                uiState = "A2A HITL: Input Required"
                pendingHitlTask = task
                val question =
                    task.status.message
                        ?.parts
                        ?.filterIsInstance<TextPart>()
                        ?.joinToString(" ") { it.text }
                        ?: "Remote agent requires additional human feedback"
                taskResult = "Agent Question: $question"
                addLog("A2A HITL Interrupt → Agent asks: '$question' (Task ID: ${task.id})")
            }

            A2ATaskState.FAILED -> {
                uiState = "Task Failed"
                pendingHitlTask = null
                val error =
                    task.status.message
                        ?.parts
                        ?.filterIsInstance<TextPart>()
                        ?.joinToString(" ") { it.text }
                        ?: "Task execution failed on remote server"
                taskResult = "Error: $error"
                addLog("Task ${task.id} Failed → $error")
            }

            else -> {
                uiState = "Task Status: ${task.status.state}"
            }
        }
    }

    private fun addLog(message: String) {
        _eventLog.value = _eventLog.value + message
    }
}

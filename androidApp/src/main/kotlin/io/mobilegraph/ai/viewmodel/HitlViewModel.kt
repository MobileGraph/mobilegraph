package io.mobilegraph.ai.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mobilegraph.agents.Agent
import io.mobilegraph.agents.AgentNode
import io.mobilegraph.agents.DefaultAgentRuntime
import io.mobilegraph.ai.ApplicationLogger
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.checkpoint.InMemoryCheckpointStore
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.facade.events
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.graph.DefaultExecutionEngine
import io.mobilegraph.graph.EndNode
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.HumanReviewNode
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.graph.stateGraph
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.UserMessage
import io.mobilegraph.models.facade.chat
import io.mobilegraph.models.facade.models
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.parsers.asText
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating Human-in-the-Loop (HITL) workflows.
 *
 * ARCHITECTURE:
 * [writer] -> [review] -> [publisher] OR back to [writer]
 *
 * FLOW:
 * 1. Agent generates content.
 * 2. Execution pauses at 'review' node (HumanReviewNode).
 * 3. User provides approval/feedback.
 * 4. Workflow resumes:
 *    - If approved: Transitions to 'publisher' (EndNode).
 *    - If rejected: Transitions back to 'writer' with feedback.
 */
class HitlViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var currentOutput by mutableStateOf("")
    var awaitingReview by mutableStateOf<ExecutionResult.AwaitingReview?>(null)

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog
    val checkpointStore = InMemoryCheckpointStore()
    private val executionEngine = DefaultExecutionEngine(checkpointStore)
    private val agentRuntime = DefaultAgentRuntime(executionEngine, checkpointStore)
    private var isInitialized = false

    private lateinit var workflowGraph: StateGraph

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize {
            val chatModel = OpenAIChatModel(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o")
            withModels {
                chat("gpt-4o", chatModel) {
                    isDefault = true
                    middleware { +LoggingMiddleware(ApplicationLogger()) }
                }
            }
        }

        // Define a simple workflow with a human review step
        val model = MobileGraph.instance.models.chat()
        val writerAgent = ArticleWriterAgent(model)

        workflowGraph =
            stateGraph {
                start("writer")
                node(AgentNode("writer", writerAgent, agentRuntime))
                node(HumanReviewNode("review"))
                node(EndNode("publisher"))

                edge("writer", "review")

                // Branch based on approval
                conditionalEdge("review", "publisher") { state ->
                    state.variables["approved"] == true
                }
                conditionalEdge("review", "writer") { state ->
                    state.variables["approved"] == false
                }
            }

        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.WaitingForReview -> "Waiting for human review at ${event.nodeId}"
                        is MobileGraphEvent.NodeStarted -> "Started node: ${event.nodeId}"
                        is MobileGraphEvent.NodeCompleted -> "Completed node: ${event.nodeId}"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }
    }

    fun startWorkflow(topic: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Generating content..."
            awaitingReview = null

            try {
                val initialState =
                    SimpleGraphState(
                        executionContext = createNewContext(),
                        userQuery = topic,
                    )

                val result = agentRuntime.run(workflowGraph, initialState)
                handleResult(result)
            } catch (e: Exception) {
                uiState = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun submitReview(
        approved: Boolean,
        feedback: String,
    ) {
        val review = awaitingReview ?: return
        viewModelScope.launch {
            isLoading = true
            uiState = if (approved) "Publishing..." else "Sending back for revision..."
            awaitingReview = null

            try {
                // Resume from the review node with user input
                val traceId = review.state.executionContext.traceId.value
                val nodeId = review.nodeId
                val checkpointId = "${traceId}_${nodeId}_0" // Simplification for sample

                val result =
                    agentRuntime.resume(
                        graph = workflowGraph,
                        checkpointId = checkpointId,
                        nodeId = nodeId,
                        input = mapOf("approved" to approved, "feedback" to feedback),
                    )
                handleResult(result)
            } catch (e: Exception) {
                uiState = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun handleResult(result: ExecutionResult) {
        when (result) {
            is ExecutionResult.Success -> {
                uiState = "Workflow Completed"
                currentOutput = result.state.variables["content"] as? String ?: "No content"
            }

            is ExecutionResult.AwaitingReview -> {
                uiState = "Awaiting Review"
                awaitingReview = result
                currentOutput = result.state.variables["content"] as? String ?: "No content"
            }

            else -> {}
        }
    }

    private fun createNewContext() =
        SimpleExecutionContext(
            traceId = TraceId("hitl-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

class ArticleWriterAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Writer"
    override val description = "Writes articles"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a helpful writer. Write a short paragraph about the topic.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage {
        val feedback = state.variables["feedback"] as? String
        val topic = state.userQuery
        return if (feedback != null) {
            UserMessage("Rewrite about '$topic' considering this feedback: $feedback")
        } else {
            UserMessage("Write a paragraph about '$topic'")
        }
    }

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("content" to output.asText()))
}

data class SimpleGraphState(
    override val executionContext: io.mobilegraph.core.context.ExecutionContext,
    override val variables: Map<String, Any?> = emptyMap(),
    override val userQuery: String = "",
) : GraphState {
    override fun copy(
        variables: Map<String, Any?>,
        userQuery: String,
    ): SimpleGraphState = copy(executionContext = executionContext, variables = variables, userQuery = userQuery)
}

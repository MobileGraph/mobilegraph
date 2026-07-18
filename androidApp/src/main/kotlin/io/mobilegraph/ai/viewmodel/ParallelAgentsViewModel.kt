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
 * ViewModel demonstrating Parallel Execution (Fan-out / Fan-in).
 *
 * ARCHITECTURE:
 *       (orchestrator)
 *             |
 *      +------+------+
 *      |             |
 * (researcher)    (creative)  <-- Parallel Execution
 *      |             |
 *      +------+------+
 *             |
 *        (aggregator)         <-- JoinNode (Fan-in)
 *             |
 *        (publisher)
 *
 * KEY FEATURES:
 * 1. Parallelism: Researcher and Creative agents run at the same time.
 * 2. Synchronization: JoinNode waits for BOTH branches before proceeding.
 * 3. State Merging: Combines data from different branches into a unified state.
 */
class ParallelAgentsViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var researchOutput by mutableStateOf("")
    var creativeOutput by mutableStateOf("")
    var finalSummary by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val checkpointStore = InMemoryCheckpointStore()
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

        val model = MobileGraph.instance.models.chat()

        // 1. Define Parallel Agents
        val researcher = ResearcherAgent(model)
        val creativeWriter = CreativeWriterAgent(model)

        // 2. Build the Fan-out / Fan-in Graph
        workflowGraph =
            stateGraph {
                start("orchestrator")

                // Entry node
                node(OrchestratorNode("orchestrator"))

                // Fan-out: Parallel execution of Researcher and Creative Writer
                node(AgentNode("researcher", researcher, agentRuntime))
                node(AgentNode("creative", creativeWriter, agentRuntime))

                // Fan-in: Synchronization point
                join("aggregator", incomingCount = 2) { states ->
                    // Merge logic: Combine results from all parallel branches
                    val allVariables = mutableMapOf<String, Any?>()
                    states.forEach { allVariables.putAll(it.variables) }

                    states.first().copy(variables = allVariables)
                }

                node(EndNode("publisher"))

                // Transitions
                edge("orchestrator", "researcher")
                edge("orchestrator", "creative")

                edge("researcher", "aggregator")
                edge("creative", "aggregator")

                edge("aggregator", "publisher")
            }

        // Event logging
        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.NodeStarted -> "Started: ${event.nodeId}"
                        is MobileGraphEvent.NodeCompleted -> "Finished: ${event.nodeId}"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }
    }

    fun runParallelWorkflow(topic: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Executing parallel agents..."

            try {
                val initialState =
                    SimpleGraphState(
                        executionContext = createNewContext(),
                        userQuery = topic,
                    )

                val result = agentRuntime.run(workflowGraph, initialState)

                if (result is ExecutionResult.Success) {
                    uiState = "Completed"
                    researchOutput = result.state.variables["research_data"] as? String ?: ""
                    creativeOutput = result.state.variables["creative_content"] as? String ?: ""
                    finalSummary = "Success! Parallel work integrated."
                }
            } catch (e: Exception) {
                uiState = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun createNewContext() =
        SimpleExecutionContext(
            traceId = TraceId("parallel-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

/**
 * Agent focused on gathering factual data.
 */
class ResearcherAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Researcher"
    override val description = "Gathers facts"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a researcher. Provide 3 key facts about the topic.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage("Research the topic: ${state.userQuery}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("research_data" to output.asText()))
}

/**
 * Agent focused on creative expression.
 */
class CreativeWriterAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Writer"
    override val description = "Writes creatively"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a poet. Write a short poem about the topic.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage("Write about: ${state.userQuery}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("creative_content" to output.asText()))
}

/**
 * Simple node to start the workflow.
 */
class OrchestratorNode(
    override val id: String,
) : io.mobilegraph.graph.GraphNode {
    override suspend fun execute(state: GraphState) = ExecutionResult.Success(state)
}

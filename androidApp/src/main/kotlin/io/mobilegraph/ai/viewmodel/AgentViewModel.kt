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
import io.mobilegraph.core.context.ExecutionContext
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
import io.mobilegraph.graph.GraphNode
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
import io.mobilegraph.parsers.ParseResult
import io.mobilegraph.parsers.asText
import io.mobilegraph.parsers.structuredOutputParser
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * ViewModel demonstrating Agent Orchestration and Structured Output.
 *
 * ARCHITECTURE:
 * [planner] -> [worker] -> (conditional loop) -> [end]
 *
 * KEY FEATURES:
 * 1. Structured Output: Uses StructuredOutputParser (JSON) to get a type-safe plan.
 * 2. Iterative Execution: The 'worker' node loops based on the plan steps.
 * 3. State Management: Uses a custom AppAgentState to track the plan and results.
 */
class AgentViewModel : ViewModel() {
    private var isInitialized = false
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var currentOutput by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val executionEngine = DefaultExecutionEngine()
    private val agentRuntime = DefaultAgentRuntime(executionEngine)

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize(context) {
            val chatModel = OpenAIChatModel(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o")
            withModels {
                chat("gpt-4o", chatModel) {
                    isDefault = true
                    defaultConfig {
                        temperature = 0.2f
                        maxTokens = 1024
                    }
                    middleware { +LoggingMiddleware(ApplicationLogger()) }
                }
            }
        }

        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.AgentStarted -> "Agent execution started"
                        is MobileGraphEvent.AgentCompleted -> "Agent execution completed"
                        is MobileGraphEvent.AgentFailed -> "Agent execution failed: ${event.error}"
                        is MobileGraphEvent.NodeStarted -> "Executing node: ${event.nodeId}"
                        is MobileGraphEvent.NodeCompleted -> "Completed node: ${event.nodeId}"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }
    }

    fun runOrchestratedAgent(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) return@launch
            isLoading = true
            uiState = "Orchestrating..."

            try {
                val context =
                    SimpleExecutionContext(
                        traceId = TraceId("agent-trace-${Random.nextInt()}"),
                        requestId = RequestId("agent-req-${Random.nextInt()}"),
                    )

                val model = MobileGraph.instance.models.chat()

                // 1. Define custom agents at the App Layer
                val plannerAgent = AppPlannerAgent(model)
                val workerAgent = AppWorkerAgent(model)

                // 2. Build the orchestration graph
                val supervisorGraph =
                    stateGraph {
                        start("planner")

                        node(AgentNode("planner", plannerAgent, agentRuntime))
                        node(AgentNode("worker", workerAgent, agentRuntime))
                        node(EndNode("end"))

                        edge("planner", "worker")

                        // Simple loop: if currentStep < total steps
                        conditionalEdge("worker", "worker") { state ->
                            val s = state as AppAgentState
                            s.currentStep < s.plan.size
                        }

                        conditionalEdge("worker", "end") { state ->
                            val s = state as AppAgentState
                            s.currentStep >= s.plan.size
                        }
                    }

                // 3. Initialize State
                val initialState =
                    AppAgentState(
                        executionContext = context,
                        userQuery = query,
                    )

                // 4. Run the Orchestrator
                val result = agentRuntime.run(supervisorGraph, initialState)
                // 5. Show Output
                when (result) {
                    is ExecutionResult.Success -> {
                        val finalState = result.state as AppAgentState
                        val resultsText =
                            finalState.results
                                .map {
                                    "- ${it.key}: ${it.value}"
                                }.joinToString("\n")
                        currentOutput = "Orchestration Complete!\n\n" +
                            "Plan: ${finalState.plan.joinToString(" -> ")}\n\n" +
                            "Results:\n$resultsText"
                        uiState = "Orchestration Complete"
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                uiState = "Error: ${e.message}"
                addEvent("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

// --- App Layer Agent Implementations ---

@Serializable
data class PlanResponse(
    val tasks: List<String>,
    val complexity: String,
)

class AppPlannerAgent(
    override val model: ChatModel,
) : Agent {
    override val name: String = "Planner"
    override val description: String = "Plans tasks"
    override val tools: ToolRegistry? = null

    private val parser = structuredOutputParser<PlanResponse>()

    override val rolePrompt: SystemMessage =
        SystemMessage(
            "You are a planner. Generate exactly 2 tasks for the given query. " +
                "${parser.formatInstructions()}",
        )
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage("Plan tasks for: ${state.userQuery}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState =
        when (val result = parser.parse(output)) {
            is ParseResult.Success -> {
                (state as AppAgentState).updatePlan(result.value.tasks)
            }

            is ParseResult.Failure -> {
                // Fallback or error handling
                (state as AppAgentState).updatePlan(listOf("Error parsing plan: ${result.error.message}"))
            }

            is ParseResult.Partial -> {
                (state as AppAgentState).updatePlan(result.partialValue?.tasks ?: emptyList())
            }
        }
}

class AppWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name: String = "Worker"
    override val description: String = "Executes tasks"
    override val tools: ToolRegistry? = null
    override val rolePrompt: SystemMessage = SystemMessage("You are a worker. Summarize the task being executed.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage {
        val s = state as AppAgentState
        val currentTask = s.plan.getOrNull(s.currentStep) ?: "None"
        return UserMessage("Execute this task: $currentTask")
    }

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState {
        val s = state as AppAgentState
        val currentTask = s.plan.getOrNull(s.currentStep) ?: "None"
        return s.addResult(currentTask, output.asText()).nextStep()
    }
}

/**
 * App-specific state implementation.
 * The SDK only knows about [GraphState], but the app can define its own structure.
 */
data class AppAgentState(
    override val executionContext: ExecutionContext,
    override val variables: Map<String, Any?> = emptyMap(),
    override val userQuery: String = "",
    val messages: List<String> = emptyList(),
    val plan: List<String> = emptyList(),
    val currentStep: Int = 0,
    val results: Map<String, String> = emptyMap(),
) : GraphState {
    override fun copy(
        variables: Map<String, Any?>,
        userQuery: String,
    ): AppAgentState = copy(executionContext = executionContext, variables = variables, userQuery = userQuery)

    fun updatePlan(newPlan: List<String>) = copy(plan = newPlan)

    fun nextStep() = copy(currentStep = currentStep + 1)

    fun addResult(
        key: String,
        value: String,
    ) = copy(results = results + (key to value))
}

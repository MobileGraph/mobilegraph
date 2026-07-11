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
import io.mobilegraph.core.facade.MobileGraph
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
import io.mobilegraph.models.facade.claude
import io.mobilegraph.models.facade.models
import io.mobilegraph.models.facade.openai
import io.mobilegraph.models.facade.router
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.parsers.asText
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating Agents utilizing a Model Router for
 * intelligent brain selection (Manager vs Worker models).
 *
 * GRAPH STRUCTURE:
 *
 * [ROUTED WORKFLOW]
 *       (manager)  <-- Routed to CLAUDE (High Reasoning)
 *          |
 *       (worker)   <-- Routed to GPT-4O-MINI (Fast/Cheap)
 *          |
 *        (end)
 */
class AgentRouterViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var finalResult by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val checkpointStore = InMemoryCheckpointStore()
    private val executionEngine = DefaultExecutionEngine(checkpointStore)
    private val agentRuntime = DefaultAgentRuntime(executionEngine, checkpointStore)
    private var isInitialized = false

    private lateinit var workflow: StateGraph

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize(context) {
            withModels {
                // 1. Register individual providers
                openai(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o-mini")
                claude(apiKey = BuildConfig.ANTHROPIC_API_KEY, name = "claude-sonnet-4-5-20250929")

                // 2. Setup the "Brain Router"
                router("smart-brain") {
                    // Rule: If the prompt comes from a "Manager", use Claude
                    policy {
                        condition { it.prompt.messages.any { msg -> msg is SystemMessage && msg.content.contains("Manager") } }
                        use("claude-sonnet-4-5-20250929")
                    }
                    // Default for all other agents (Workers)
                    default("gpt-4o-mini")
                }
            }
        }

        val router = MobileGraph.instance.models.chat("smart-brain")

        // 3. Define Agents that all use the SAME router instance
        val manager = RouterManagerAgent(router)
        val worker = RouterWorkerAgent(router)

        // 4. Build the workflow
        workflow =
            stateGraph {
                start("manager")
                node(AgentNode("manager", manager, agentRuntime))
                node(AgentNode("worker", worker, agentRuntime))
                node(EndNode("end"))

                edge("manager", "worker")
                edge("worker", "end")
            }
    }

    fun runRoutedWorkflow(query: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Executing with Routed Brains..."
            finalResult = ""

            try {
                val initialState =
                    SimpleGraphState(
                        executionContext =
                            SimpleExecutionContext(
                                traceId = TraceId("router-agent-${Random.nextInt()}"),
                                requestId = RequestId("req-${Random.nextInt()}"),
                            ),
                        userQuery = query,
                    )

                val result = agentRuntime.run(workflow, initialState)
                if (result is ExecutionResult.Success) {
                    uiState = "Completed"
                    finalResult = result.state.variables["final_output"] as? String ?: "No output"
                }
            } catch (e: Exception) {
                uiState = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

/**
 * High-reasoning agent that identifies as "Manager".
 * The router will detect this and switch to Claude.
 */
class RouterManagerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Manager"
    override val description = "Orchestrator"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a High-Level Manager. Create a 3-step strategy for the user's query.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState) = UserMessage(state.userQuery)

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("strategy" to output.asText()))
}

/**
 * Fast worker agent.
 * The router will use the default model (GPT-4o-mini).
 */
class RouterWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Worker"
    override val description = "Executor"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a Fast Worker. Turn the provided strategy into a short summary.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage {
        val strategy = state.variables["strategy"] as? String ?: "No strategy"
        return UserMessage("Summarize this: $strategy")
    }

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("final_output" to output.asText()))
}

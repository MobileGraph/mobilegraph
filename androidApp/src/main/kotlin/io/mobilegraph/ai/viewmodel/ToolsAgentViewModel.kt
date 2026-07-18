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
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.facade.events
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
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
import io.mobilegraph.models.middleware.ToolSelectionMiddleware
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.parsers.asText
import io.mobilegraph.state.GraphState
import io.mobilegraph.tools.facade.withTools
import io.mobilegraph.tools.registry.DefaultToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * ViewModel demonstrating Autonomous Tool usage by Agents.
 *
 * ARCHITECTURE:
 * [agent] -> [end]
 *
 * KEY FEATURES:
 * 1. Tool Registry: Mix of Global tools (Weather) and Local tools (Calculator).
 * 2. Autonomous Loop: The LLM decides when to call a tool based on the query.
 * 3. Implicit Discovery: Agent opts into global tools via 'useGlobalTools = true'.
 */
class ToolsAgentViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var agentResponse by mutableStateOf("")

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
            // 1. Register a Global Tool during initialization
            withTools {
                register(WeatherTool())
            }

            val chatModel = OpenAIChatModel(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o")
            withModels {
                chat("gpt-4o", chatModel) {
                    isDefault = true
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        // Use ToolSelectionMiddleware for dynamic global tool discovery
                        +ToolSelectionMiddleware()
                    }
                    defaultConfig {
                        temperature = 0.2f
                        maxTokens = 200
                    }
                }
            }
        }

        val model = MobileGraph.instance.models.chat()

        // 2. Register a Local Tool (specific to this agent)
        val toolRegistry = DefaultToolRegistry()
        toolRegistry.register(CalculatorTool())

        // 3. Define the Tool-Enabled Agent
        val toolsAgent = ToolsEnabledAgent(model, toolRegistry)

        // 4. Build a simple graph
        workflowGraph =
            stateGraph {
                start("agent")
                node(AgentNode("agent", toolsAgent, agentRuntime))
                node(EndNode("end"))
                edge("agent", "end")
            }

        // Event logging
        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.NodeStarted -> "Agent thinking..."
                        is MobileGraphEvent.NodeCompleted -> "Agent finished task"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }
    }

    fun runAgent(query: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Agent is working..."
            agentResponse = ""

            try {
                val initialState =
                    SimpleGraphState(
                        executionContext = createNewContext(),
                        userQuery = query,
                    )

                val result = agentRuntime.run(workflowGraph, initialState)

                if (result is ExecutionResult.Success) {
                    uiState = "Task Completed"
                    agentResponse = result.state.variables["response"] as? String ?: "No response"
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
            traceId = TraceId("tools-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

/**
 * An Agent implementation that provides a ToolRegistry and opts into Global tools.
 */
class ToolsEnabledAgent(
    override val model: ChatModel,
    override val tools: ToolRegistry,
) : Agent {
    override val name = "Tool Expert"
    override val description = "An agent that uses tools to answer questions"
    override val rolePrompt =
        SystemMessage("You are a helpful assistant with access to tools. Use them when necessary to provide accurate information.")
    override val graph: StateGraph? = null

    // Opt-in to Global Tools
    override val useGlobalTools: Boolean = true

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage(state.userQuery)

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("response" to output.asText()))
}

// --- Sample Tools ---

@Serializable
data class WeatherInput(
    val city: String,
)

class WeatherTool : Tool<WeatherInput, String> {
    override val metadata =
        ToolMetadata(
            name = "get_weather",
            description = "Gets the current weather for a specific city.",
        )
    override val inputSerializer = WeatherInput.serializer()

    override suspend fun invoke(
        input: WeatherInput,
        context: ExecutionContext,
    ): String {
        // In a real app, this would call a weather API
        return "The weather in ${input.city} is 25°C and Sunny."
    }
}

@Serializable
data class CalcInput(
    val expression: String,
)

class CalculatorTool : Tool<CalcInput, String> {
    override val metadata =
        ToolMetadata(
            name = "calculate",
            description = "Performs mathematical calculations.",
        )
    override val inputSerializer = CalcInput.serializer()

    override suspend fun invoke(
        input: CalcInput,
        context: ExecutionContext,
    ): String = "Result of '${input.expression}' is 42 (calculated by tool)"
}

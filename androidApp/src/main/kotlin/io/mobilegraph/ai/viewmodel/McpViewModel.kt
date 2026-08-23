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
import io.mobilegraph.core.tools.tools
import io.mobilegraph.graph.DefaultExecutionEngine
import io.mobilegraph.graph.EndNode
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.graph.stateGraph
import io.mobilegraph.mcp.McpPlugin
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating MCP (Model Context Protocol) integration.
 */
class McpViewModel : ViewModel() {
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
            // 1. Initialize Tools
            withTools {
            }

            // 2. Install MCP Plugin
            plugins {
                install(McpPlugin.Mcp) {
                    streamableHttpServer("https://remote-mcp-server-authless.mobilegraph-mcp-test.workers.dev/mcp") {
                        header("X-Custom", "Val")
                    }
                }
            }

            val chatModel = OpenAIChatModel(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o")
            withModels {
                chat("gpt-4o", chatModel) {
                    isDefault = true
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                    defaultConfig {
                        temperature = 0.2f
                        maxTokens = 500
                    }
                }
            }
        }

        // 2. Trigger MCP Discovery
        val mcpIntegration = MobileGraph.instance.environment.getComponent(McpPlugin.McpIntegration::class)
        mcpIntegration?.initialize(MobileGraph.tools.registry())

        val model = MobileGraph.instance.models.chat()

        // 3. Define the Agent
        val mcpAgent = ToolsEnabledAgent(model, MobileGraph.tools.registry())
        val calculatorAgent = CalculatorAgent(model, tools = MobileGraph.tools.registry())

        // 4. Build graph
        workflowGraph =
            stateGraph {
                start("agent")
                node(AgentNode("agent", mcpAgent, agentRuntime))
                node(AgentNode(calculatorAgent.name, calculatorAgent, agentRuntime))
                node(EndNode("end"))
                edge("agent", calculatorAgent.name)
                edge(calculatorAgent.name, "end")
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
            uiState = "Agent is working with MCP tools..."
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
                    agentResponse =
                        (
                            result.state.variables["response"] as? String
                                ?: "No response"
                        ) + ("\n Response using MCP Tool: ") + (result.state.variables["tool_response"])
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
            traceId = TraceId("mcp-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

class CalculatorAgent(
    override val model: ChatModel,
    override val tools: ToolRegistry?,
) : Agent {
    override val name = "Calculator"
    override val description = "An agent that uses tools to perform calculation"
    override val rolePrompt =
        SystemMessage("You are a helpful assistant with access to tools. Use them when necessary to provide accurate calculation result.")
    override val graph: StateGraph? = null

    // Opt-in to Global Tools
    override val useGlobalTools: Boolean = true

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage("Task: 1. add 50 and 60. 2. Multiply 500 and 600")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("tool_response" to output.asText()))
}

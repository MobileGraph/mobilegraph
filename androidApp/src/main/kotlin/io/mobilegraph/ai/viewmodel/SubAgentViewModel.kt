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
 * ViewModel demonstrating complex hierarchical orchestration:
 * Main Graph -> Parallel Team Managers -> Internal Team Sub-Graphs.
 *
 * GRAPH STRUCTURE:
 *
 * [MAIN WORKFLOW]
 *       (entry)
 *          |
 *    +-----+-----+
 *    |           |
 * (research_team) (creative_team)  <-- Parallel Sub-Agents (each has its own internal graph)
 *    |           |
 *    |           | [Sub-Graph 1: Research]    [Sub-Graph 2: Creative]
 *    |           | (research_task)            (drafting)
 *    |           |      |                        |
 *    |           | (verification)             (editing)
 *    |           |      |                        |
 *    |           | `research_done`            `creative_done`
 *    |           |
 *    +-----+-----+
 *          |
 *     (aggregator)  <-- Fan-In (JoinNode)
 *          |
 *   (final_publish) <-- EndNode
 */
class SubAgentViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var finalResult by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val checkpointStore = InMemoryCheckpointStore()
    private val executionEngine = DefaultExecutionEngine(checkpointStore)
    private val agentRuntime = DefaultAgentRuntime(executionEngine, checkpointStore)
    private var isInitialized = false

    private lateinit var mainWorkflow: StateGraph

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

        // --- SUB-GRAPH 1: Research Team ---
        val researchWorker = ResearchWorkerAgent(model)
        val factChecker = FactCheckerWorkerAgent(model)

        val researchTeamGraph =
            stateGraph {
                start("research_task")
                node(AgentNode("research_task", researchWorker, agentRuntime))
                node(AgentNode("verification", factChecker, agentRuntime))
                node(EndNode("research_done"))
                edge("research_task", "verification")
                edge("verification", "research_done")
            }
        val researchManager = TeamLeadAgent("Research Lead", model, researchTeamGraph)

        // --- SUB-GRAPH 2: Creative Team ---
        val poet = PoetWorkerAgent(model)
        val editor = CreativeEditorWorkerAgent(model)

        val creativeTeamGraph =
            stateGraph {
                start("drafting")
                node(AgentNode("drafting", poet, agentRuntime))
                node(AgentNode("editing", editor, agentRuntime))
                node(EndNode("creative_done"))
                edge("drafting", "editing")
                edge("editing", "creative_done")
            }
        val creativeManager = TeamLeadAgent("Creative Lead", model, creativeTeamGraph)

        // --- MAIN WORKFLOW: Top-Level Orchestration ---
        mainWorkflow =
            stateGraph {
                start("entry")

                // Simple entry node
                node(HierarchicalOrchestratorNode("entry"))

                // Fan-out to Sub-Agents (each has its own sub-graph)
                node(AgentNode("research_team", researchManager, agentRuntime))
                node(AgentNode("creative_team", creativeManager, agentRuntime))

                // Fan-in: Wait for both teams to finish their entire workflows
                join("aggregator", incomingCount = 2) { states ->
                    val combined = mutableMapOf<String, Any?>()
                    states.forEach { combined.putAll(it.variables) }
                    states.first().copy(variables = combined)
                }

                node(EndNode("final_publish"))

                // Connections
                edge("entry", "research_team")
                edge("entry", "creative_team")

                edge("research_team", "aggregator")
                edge("creative_team", "aggregator")

                edge("aggregator", "final_publish")
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

    fun runHierarchicalWorkflow(topic: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Top Orchestrator is starting teams..."
            finalResult = ""

            try {
                val initialState =
                    SimpleGraphState(
                        executionContext = createNewContext(),
                        userQuery = topic,
                    )

                val result = agentRuntime.run(mainWorkflow, initialState)

                if (result is ExecutionResult.Success) {
                    uiState = "Full Workflow Completed"
                    val facts = result.state.variables["verified_research"] as? String ?: ""
                    val poem = result.state.variables["edited_poem"] as? String ?: ""
                    finalResult = "FACTS:\n$facts\n\nPOEM:\n$poem"
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
            traceId = TraceId("complex-sub-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

// --- Agent Implementations ---

/**
 * A Generic Agent that orchestrates a sub-graph.
 */
class TeamLeadAgent(
    override val name: String,
    override val model: ChatModel,
    override val graph: StateGraph,
) : Agent {
    override val description = "Orchestrates a specialized sub-team workflow"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are the $name. You ensure your team's workflow completes successfully.")

    override fun formatAgentInstruction(state: GraphState) = UserMessage("")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ) = state
}

/**
 * Worker Agent: Research
 */
class ResearchWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Researcher"
    override val description = "Fact gathering"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a researcher. Provide 3 factual bullets.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState) = UserMessage("Research: ${state.userQuery}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ) = state.copy(variables = state.variables + ("raw_research" to output.asText()))
}

/**
 * Worker Agent: Fact Checker
 */
class FactCheckerWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Fact Checker"
    override val description = "Verifies research"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a fact checker. Verify and format these research notes.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState) = UserMessage("Check these notes: ${state.variables["raw_research"]}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ) = state.copy(variables = state.variables + ("verified_research" to output.asText()))
}

/**
 * Worker Agent: Poet
 */
class PoetWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Poet"
    override val description = "Creative drafting"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a poet. Write a short poem.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState) = UserMessage("Write poem about: ${state.userQuery}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ) = state.copy(variables = state.variables + ("raw_poem" to output.asText()))
}

/**
 * Worker Agent: Editor
 */
class CreativeEditorWorkerAgent(
    override val model: ChatModel,
) : Agent {
    override val name = "Editor"
    override val description = "Refines creative work"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a creative editor. Refine and polish the poem provided.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState) = UserMessage("Polish this poem: ${state.variables["raw_poem"]}")

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ) = state.copy(variables = state.variables + ("edited_poem" to output.asText()))
}

/**
 * Entry node implementation for hierarchical sample
 */
class HierarchicalOrchestratorNode(
    override val id: String,
) : io.mobilegraph.graph.GraphNode {
    override suspend fun execute(state: GraphState) = ExecutionResult.Success(state)
}

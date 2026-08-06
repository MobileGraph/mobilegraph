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
import io.mobilegraph.core.facade.MobileGraph
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
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.parsers.asText
import io.mobilegraph.skills.Skill
import io.mobilegraph.skills.SkillLoader
import io.mobilegraph.state.GraphState
import io.mobilegraph.tools.facade.withTools
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * ViewModel demonstrating the Skills System (Declarative AI).
 */
class SkillViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var agentResponse by mutableStateOf("")
    var skillWarnings = mutableListOf<String>()

    private val checkpointStore = InMemoryCheckpointStore()
    private val executionEngine = DefaultExecutionEngine(checkpointStore)
    private val agentRuntime = DefaultAgentRuntime(executionEngine, checkpointStore)
    private var isInitialized = false

    private lateinit var workflowGraph: StateGraph

    // Sample Skill Content (Normally loaded from URL or Assets)
    private val timeManagementSkillMd =
        """
        # Time Management
        A skill for setting reminders and managing daily tasks.
        
        ## Instructions
        - Set Priority as Medium
        - If a reminder is set for a time that has already passed, warn the user.
        - Be encouraging and helpful.
        
        ## Tools
        - `set_reminder`: Schedules a new reminder.
        """.trimIndent()

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize {
            withTools {
                // Global tools could be here
            }

            val chatModel = OpenAIChatModel(apiKey = BuildConfig.OPEN_AI_API_KEY, name = "gpt-4o")
            withModels {
                chat("gpt-4o", chatModel) {
                    isDefault = true
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                    }
                }
            }
        }

        // 1. Load the Skill from Markdown
        val loadResult =
            SkillLoader.fromMarkdown(
                content = timeManagementSkillMd,
                tools = listOf(ReminderTool()),
            )

        skillWarnings.addAll(loadResult.warnings)
        val timeSkill = loadResult.skill

        val model = MobileGraph.instance.models.chat()

        // 2. Define the Agent equipped with the Skill
        val assistantAgent = SkilledAssistantAgent(model, listOf(timeSkill))

        // 3. Build graph
        workflowGraph =
            stateGraph {
                start("agent")
                node(AgentNode("agent", assistantAgent, agentRuntime))
                node(EndNode("end"))
                edge("agent", "end")
            }
    }

    fun runAgent(query: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Agent is working with its Skills..."
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
            traceId = TraceId("skill-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )
}

/**
 * An Agent implementation that accepts a list of Skills.
 */
class SkilledAssistantAgent(
    override val model: ChatModel,
    override val skills: List<Skill>,
) : Agent {
    override val name = "Skilled Assistant"
    override val description = "An agent equipped with modular skills"
    override val tools: ToolRegistry? = null
    override val rolePrompt = SystemMessage("You are a helpful personal assistant.")
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage = UserMessage(state.userQuery)

    override suspend fun handleLlmOutput(
        output: ModelOutput,
        state: GraphState,
    ): GraphState = state.copy(variables = state.variables + ("response" to output.asText()))
}

// --- Sample Tool for the Skill ---

@Serializable
data class ReminderInput(
    val task: String,
    val priority: String,
)

class ReminderTool : Tool<ReminderInput, String> {
    override val metadata =
        ToolMetadata(
            name = "set_reminder",
            description = "Schedules a new reminder for the user.",
        )
    override val inputSerializer = ReminderInput.serializer()

    override suspend fun invoke(
        input: ReminderInput,
        context: ExecutionContext,
    ): String = "SUCCESS: Reminder for '${input.task}' set with [${input.priority}] priority."
}

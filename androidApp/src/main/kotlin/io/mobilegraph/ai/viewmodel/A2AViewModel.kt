package io.mobilegraph.ai.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.a2a.adapter.A2AAgentDescriptor
import io.mobilegraph.a2a.adapter.A2ATaskAdapter
import io.mobilegraph.a2a.client.A2AAuthProvider
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.client.A2AClientConfig
import io.mobilegraph.a2a.client.BearerTokenAuthProvider
import io.mobilegraph.a2a.client.NoAuthProvider
import io.mobilegraph.a2a.facade.a2a
import io.mobilegraph.a2a.node.a2aNode
import io.mobilegraph.ai.ApplicationLogger
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.checkpoint.InMemoryCheckpointStore
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.facade.events
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.graph.DefaultExecutionEngine
import io.mobilegraph.graph.EndNode
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.GraphNode
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.graph.stateGraph
import io.mobilegraph.models.facade.chat
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating A2A (Agent-to-Agent) Protocol Integration with the A2A Multi-Skill Test Server.
 *
 * Demonstrates:
 * 1. Discovering a remote multi-skill A2A agent via its Agent Card (`/.well-known/agent-card.json`)
 * 2. Interacting with diverse agent skills (Echo, Math Calculator, Greeting Generator, OpenAI Chat)
 * 3. Creating an A2A Graph Node (`a2aNode`) to delegate multi-modal or text tasks
 * 4. Integrating A2A delegation into a multi-step MobileGraph `StateGraph` workflow
 * 5. Listening to live A2A lifecycle events published by the MobileGraph runtime
 */
class A2AViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var agentDescriptor by mutableStateOf<A2AAgentDescriptor?>(null)
    var taskResult by mutableStateOf("")
    var remoteAgentUrl by mutableStateOf("https://a2a-test-server-679161232357.us-central1.run.app")
    var authToken by mutableStateOf("")
    var useStreaming by mutableStateOf(false)
    var awaitingReview by mutableStateOf<ExecutionResult.AwaitingReview?>(null)

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    private val checkpointStore = InMemoryCheckpointStore()
    private val executionEngine = DefaultExecutionEngine(checkpointStore)
    private val agentRuntime = io.mobilegraph.agents.DefaultAgentRuntime(executionEngine, checkpointStore)
    private var isInitialized = false
    private lateinit var workflowGraph: StateGraph

    private lateinit var a2aClient: A2AClient
    private var isMockMode = false

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // 1. Initialize MobileGraph Runtime with A2A DSL and Models
        MobileGraph.initialize {
            a2a {
                remoteAgent(remoteAgentUrl) {
                    config =
                        A2AClientConfig(
                            requestTimeoutMs = 60_000L,
                            taskPollingIntervalMs = 1_500L,
                        )
                }
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

        // Initialize default real A2A client
        a2aClient = A2AClient(HttpClient())

        // 2. Collect MobileGraph A2A Lifecycle Events for observability
        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val logEntry =
                    when (event) {
                        is MobileGraphEvent.A2ATaskSubmitted -> {
                            "A2A Task Submitted → Node: ${event.nodeId}, Agent: ${event.remoteAgent}"
                        }

                        is MobileGraphEvent.A2ATaskStatusChanged -> {
                            "A2A Status Update → Task: ${event.taskId}, Status: ${event.status}"
                        }

                        is MobileGraphEvent.A2AArtifactReceived -> {
                            "A2A Artifact Received → Task: ${event.taskId}, Artifact ID: ${event.artifactId}"
                        }

                        is MobileGraphEvent.A2ATaskCompleted -> {
                            "A2A Task Completed → Task: ${event.taskId}"
                        }

                        is MobileGraphEvent.A2ATaskFailed -> {
                            "A2A Task Failed → Task: ${event.taskId}, Error: ${event.error}"
                        }

                        is MobileGraphEvent.NodeStarted -> {
                            "Workflow Node Started: ${event.nodeId}"
                        }

                        is MobileGraphEvent.NodeCompleted -> {
                            "Workflow Node Completed: ${event.nodeId}"
                        }

                        else -> {
                            null
                        }
                    }
                logEntry?.let { addEvent(it) }
            }
        }
    }

    /**
     * Discovers the remote agent by querying its Agent Card.
     */
    fun discoverAgent(url: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Discovering Agent Card from $url..."
            addEvent("Fetching Agent Card from $url/.well-known/agent-card.json")

            try {
                val card = a2aClient.discoverAgent(url, forceRefresh = true)
                agentDescriptor = A2AAgentDescriptor.from(card)
                uiState = "Agent Discovered: ${card.name} (A2A v${card.protocolVersion ?: "0.3"})"
                addEvent("Discovered Agent: '${card.name}' with ${card.skills.size} skills")
            } catch (e: Exception) {
                uiState = "Discovery Error: ${e.message}"
                addEvent("Discovery Failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Configures the ViewModel to use a built-in Mock A2A Agent matching the Multi-Skill Test Server.
     * Allows fully offline, instant testing of all A2A features without external servers.
     */
    fun setupDemoMockAgent() {
        isMockMode = true
        remoteAgentUrl = "https://a2a-test-server-679161232357.us-central1.run.app"

        val mockAgentCardJson =
            """
            {
                "name": "A2A Multi-Skill Test Server",
                "description": "A multi-skill A2A test server supporting Echo, Math, Greeting, and OpenAI-powered chat. Compliant with A2A Protocol v1.0. Designed for testing A2A client agents.",
                "url": "https://a2a-test-server-679161232357.us-central1.run.app",
                "version": "1.0.0",
                "protocolVersion": "0.3",
                "capabilities": {
                    "streaming": true,
                    "pushNotifications": true
                },
                "defaultInputModes": ["text/plain"],
                "defaultOutputModes": ["text/plain"],
                "skills": [
                    {
                        "id": "echo_bot",
                        "name": "Echo Bot",
                        "description": "Echoes back the user's message. Useful for testing basic A2A request/response flow. Prefix with /echo to force this skill.",
                        "tags": ["echo", "test", "debug"],
                        "examples": ["hello world", "/echo test message"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    },
                    {
                        "id": "math_calculator",
                        "name": "Math Calculator",
                        "description": "Performs basic math operations (add, subtract, multiply, divide) from natural language. Prefix with /math to force this skill.",
                        "tags": ["math", "calculator", "compute"],
                        "examples": ["what is 5 + 3", "/math 10 multiply by 4", "100 divided by 5"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    },
                    {
                        "id": "greeting_generator",
                        "name": "Greeting Generator",
                        "description": "Generates personalized, context-aware greetings based on time of day and optional name. Prefix with /greet to force this skill.",
                        "tags": ["greeting", "welcome", "social"],
                        "examples": ["hello", "/greet Alice", "good morning"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    },
                    {
                        "id": "openai_chat",
                        "name": "AI Assistant (OpenAI)",
                        "description": "General-purpose AI conversational assistant powered by OpenAI. Supports streaming and complex queries. Prefix with /ai or /ask.",
                        "tags": ["ai", "llm", "chat", "openai", "gpt"],
                        "examples": ["/ai explain quantum computing in simple terms", "/ask write a haiku about Python programming"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    },
                    {
                        "id": "human_in_the_loop",
                        "name": "HITL Approval",
                        "description": "Demonstrates Human-in-the-Loop (HITL) execution flow. Pauses execution with TASK_STATE_INPUT_REQUIRED and resumes when human feedback is received. Prefix with /hitl.",
                        "tags": ["hitl", "approval", "human-in-the-loop", "interactive"],
                        "examples": ["/hitl transfer $500 to Alice", "/hitl deploy to production"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    }
                ]
            }
            """.trimIndent()

        val mockEngine =
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/.well-known/agent-card.json")) {
                    respond(
                        content = mockAgentCardJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                } else {
                    val requestBody = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    if (requestBody.contains("/hitl") && !requestBody.contains("userResponse")) {
                        val mockInputRequiredJson =
                            """
                            {
                                "jsonrpc": "2.0",
                                "id": 1,
                                "result": {
                                    "id": "task-hitl-${Random.nextInt(1000, 9999)}",
                                    "status": {
                                        "state": "input-required",
                                        "message": {
                                            "role": "agent",
                                            "parts": [
                                                {
                                                    "type": "text",
                                                    "text": "Approval required: Action requested via /hitl. Do you authorize this transaction?"
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                            """.trimIndent()
                        respond(
                            content = mockInputRequiredJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        val mockTaskCompletedJson =
                            """
                            {
                                "jsonrpc": "2.0",
                                "id": 1,
                                "result": {
                                    "id": "task-test-${Random.nextInt(1000, 9999)}",
                                    "status": {
                                        "state": "completed",
                                        "timestamp": "2026-09-02T12:00:00Z"
                                    },
                                    "artifacts": [
                                        {
                                            "artifactId": "artifact-01",
                                            "name": "Skill Processing Result",
                                            "description": "Output produced by remote A2A test skill",
                                            "parts": [
                                                {
                                                    "type": "text",
                                                    "text": "[Mock A2A Response]: Action approved and processed successfully."
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                            """.trimIndent()

                        respond(
                            content = mockTaskCompletedJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            }

        val authProvider: A2AAuthProvider =
            if (authToken.isNotBlank()) {
                BearerTokenAuthProvider(authToken)
            } else {
                NoAuthProvider()
            }

        a2aClient =
            A2AClient(
                httpClient = HttpClient(mockEngine),
                authProvider = authProvider,
            )

        discoverAgent(remoteAgentUrl)
    }

    /**
     * Executes a complete MobileGraph StateGraph workflow containing an A2A delegation node.
     */
    fun runA2AWorkflow(taskPrompt: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Executing Workflow with A2A Delegation..."
            taskResult = ""
            addEvent("Starting Graph Workflow: Local Preprocessing → A2A Node → Summary")

            try {
                val authProvider: A2AAuthProvider =
                    if (authToken.isNotBlank()) {
                        BearerTokenAuthProvider(authToken)
                    } else {
                        NoAuthProvider()
                    }

                if (!isMockMode) {
                    a2aClient =
                        A2AClient(
                            httpClient = HttpClient(),
                            authProvider = authProvider,
                        )
                }

                // 1. Build the A2A Node using the DSL
                val a2aDelegatedNode =
                    a2aNode("remote-a2a-skill-executor") {
                        this.client = a2aClient
                        this.agentUrl = remoteAgentUrl
                        this.streaming = useStreaming
                        textMessage { state ->
                            val followUp =
                                state.variables["userResponse"] as? String
                                    ?: state.variables["feedback"] as? String
                            if (!followUp.isNullOrBlank()) {
                                followUp
                            } else {
                                state.userQuery
                            }
                        }
                    }

                // 2. Build the Preprocessing Node
                val prepNode =
                    object : GraphNode {
                        override val id = "local-preprocessor"

                        override suspend fun execute(state: GraphState): ExecutionResult {
                            val sanitized = state.userQuery.trim()
                            val updated =
                                state.copy(
                                    variables = state.variables + ("sanitized_query" to sanitized),
                                )
                            return ExecutionResult.Success(updated)
                        }
                    }

                // 3. Build the Postprocessing / Summary Node
                val summaryNode =
                    object : GraphNode {
                        override val id = "workflow-summary"

                        override suspend fun execute(state: GraphState): ExecutionResult {
                            val rawResult =
                                state.variables[A2ATaskAdapter.STATE_KEY_A2A_RESULT] as? String
                                    ?: "No A2A result found"
                            val taskId = state.variables[A2ATaskAdapter.STATE_KEY_A2A_TASK_ID] ?: "unknown"
                            val formattedOutput = "Response from Remote A2A Agent (Task $taskId):\n\n$rawResult"
                            val finalState =
                                state.copy(
                                    variables = state.variables + ("final_report" to formattedOutput),
                                )
                            return ExecutionResult.Success(finalState)
                        }
                    }

                // 4. Construct the Workflow Graph
                workflowGraph =
                    stateGraph {
                        start("local-preprocessor")
                        node(prepNode)
                        node(a2aDelegatedNode)
                        node(summaryNode)
                        node(EndNode("end"))

                        edge("local-preprocessor", "remote-a2a-skill-executor")
                        edge("remote-a2a-skill-executor", "workflow-summary")
                        edge("workflow-summary", "end")
                    }

                val initialState =
                    SimpleGraphState(
                        executionContext = createNewContext(),
                        userQuery = taskPrompt,
                    )

                // 5. Execute Workflow via Agent Runtime
                val result = agentRuntime.run(workflowGraph, initialState)
                handleWorkflowResult(result)
            } catch (e: Exception) {
                uiState = "Execution Error: ${e.message}"
                addEvent("Workflow Exception: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Resumes the A2A Human-In-The-Loop (HITL) workflow with user provided follow-up input.
     */
    fun submitHitlResponse(userResponse: String) {
        val review = awaitingReview ?: return
        viewModelScope.launch {
            isLoading = true
            uiState = "Sending Follow-Up Response to A2A Agent..."
            addEvent("Submitting HITL User Response → '$userResponse'")

            try {
                val checkpointId = review.checkpointId ?: checkpointStore.getLatestCheckpointId()
                val result =
                    agentRuntime.resume(
                        graph = workflowGraph,
                        checkpointId = checkpointId,
                        nodeId = review.nodeId,
                        input = mapOf("userResponse" to userResponse, "feedback" to userResponse),
                        reExecute = true,
                    )

                handleWorkflowResult(result)
            } catch (e: Exception) {
                uiState = "HITL Resume Error: ${e.message}"
                addEvent("HITL Exception: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun handleWorkflowResult(result: ExecutionResult) {
        when (result) {
            is ExecutionResult.Success -> {
                uiState = "Workflow Completed Successfully"
                awaitingReview = null
                taskResult = result.state.variables["final_report"] as? String
                    ?: result.state.variables[A2ATaskAdapter.STATE_KEY_A2A_RESULT] as? String
                    ?: "Task completed"
            }

            is ExecutionResult.AwaitingReview -> {
                awaitingReview = result
                val question =
                    result.state.variables[A2ATaskAdapter.STATE_KEY_A2A_INPUT_REQUIRED_MESSAGE] as? String
                        ?: "Remote agent requested additional information."
                uiState = "A2A HITL: Agent Requested Information"
                taskResult = "Agent Question: $question"
                addEvent("A2A HITL Interruption: Remote agent requested input → '$question'")
            }

            is ExecutionResult.Error -> {
                uiState = "Workflow Encountered Error"
                awaitingReview = null
                taskResult = result.state.variables[A2ATaskAdapter.STATE_KEY_A2A_ERROR] as? String
                    ?: "An error occurred during workflow execution"
            }

            else -> {}
        }
    }

    private fun createNewContext() =
        SimpleExecutionContext(
            traceId = TraceId("a2a-${Random.nextInt()}"),
            requestId = RequestId("req-${Random.nextInt()}"),
        )

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

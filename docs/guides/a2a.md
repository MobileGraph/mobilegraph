# A2A Developer Guide

This guide covers how to use A2A (Agent-to-Agent) protocol support in MobileGraph.

MobileGraph provides first-class support for **A2A Protocol v1.0** (with automatic backward-compatibility for v0.3 / v0.2.1).

## Prerequisites

Add the A2A module to your project:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.mobilegraph:mobilegraph-a2a:<version>")
}
```

## 1. Discovering a Remote Agent

Every A2A agent exposes an **Agent Card** at `/.well-known/agent-card.json`:

```kotlin
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.adapter.A2AAgentDescriptor

val client = A2AClient(HttpClient())

// Discover the remote agent
val agentCard = client.discoverAgent("https://a2a-test-server-679161232357.us-central1.run.app")

// Create a typed descriptor for easy capability checking
val agent = A2AAgentDescriptor.from(agentCard)

println("Agent: ${agent.name}")
println("Protocol Version: ${agent.protocolVersion}") // e.g. "1.0.0"
println("Supports streaming: ${agent.supportsStreaming}")
println("Preferred transport: ${agent.preferredTransport}") // e.g. "JSONRPC"
println("Skills: ${agent.skills.map { it.name }}")
println("Requires auth: ${agent.requiresAuthentication}")

// Find a specific skill
val mathSkill = agent.findSkill("math_calculator")
```

## 2. Sending a Message (Synchronous - `SendMessage`)

Uses A2A v1.0 `SendMessage` method with automatic fallback to legacy `message/send`:

```kotlin
import io.mobilegraph.a2a.models.*

val task = client.sendMessage(
    agentUrl = agentCard.url,
    params = A2AMessageSendParams(
        message = A2AMessage(
            role = A2ARole.USER, // Encoded as "USER" (A2A v1.0 standard)
            parts = listOf(
                TextPart(text = "/math 10 multiply by 4")
            )
        )
    )
)

when (task.status.state) {
    A2ATaskState.COMPLETED -> {
        // Extract artifacts
        task.artifacts.forEach { artifact ->
            artifact.parts.filterIsInstance<TextPart>().forEach { part ->
                println("Result: ${part.text}")
            }
        }
    }
    A2ATaskState.INPUT_REQUIRED -> {
        println("Agent needs more info: ${task.status.message}")
    }
    A2ATaskState.FAILED -> {
        println("Task failed: ${task.status.message}")
    }
    else -> {
        println("Task status: ${task.status.state}")
    }
}
```

## 3. Streaming Task Updates (`SendStreamingMessage`)

Uses A2A v1.0 `SendStreamingMessage` method via SSE (with automatic fallback to `StreamMessage` / `message/stream`):

```kotlin
client.streamMessage(
    agentUrl = agentCard.url,
    params = A2AMessageSendParams(
        message = A2AMessage(
            role = A2ARole.USER,
            parts = listOf(TextPart(text = "/ai explain quantum computing in simple terms"))
        )
    )
).collect { event ->
    when (event) {
        is A2AStreamEvent.TaskStatusUpdate -> {
            println("Status: ${event.status.state}")
        }
        is A2AStreamEvent.TaskArtifactUpdate -> {
            println("Artifact chunk: ${event.artifact.name}")
        }
        is A2AStreamEvent.TaskComplete -> {
            println("Complete! Task ID: ${event.task.id}")
        }
        is A2AStreamEvent.StreamError -> {
            println("Error: ${event.error.message}")
        }
    }
}
```

## 4. Querying and Polling Task Status (`GetTask`)

Uses A2A v1.0 `GetTask` (with fallback to `tasks/get`):

```kotlin
// Query task status once
val task = client.getTask(
    agentUrl = agentCard.url,
    params = A2ATaskQueryParams(id = taskId)
)

// Poll task until terminal state
client.pollTask(
    agentUrl = agentCard.url,
    taskId = task.id
).collect { updatedTask ->
    println("Status: ${updatedTask.status.state}")
    if (updatedTask.status.state == A2ATaskState.COMPLETED) {
        println("Done!")
    }
}
```

## 5. Using A2ANode in a Graph

The `A2ANode` integrates A2A into MobileGraph `StateGraph` workflows:

```kotlin
import io.mobilegraph.a2a.node.a2aNode

val analyzeNode = a2aNode("remote-a2a-executor") {
    client = a2aClient
    agentUrl = "https://a2a-test-server-679161232357.us-central1.run.app"
    streaming = false // Set true for SSE streaming

    // Build the message from graph state
    textMessage { state ->
        val query = state.userQuery
        "/math $query"
    }

    // Optionally customize how results are mapped to state
    mapResult { task, state ->
        val report = task.artifacts
            .flatMap { it.parts }
            .filterIsInstance<TextPart>()
            .joinToString("\n") { it.text }
        state.copy(
            variables = state.variables + ("mathResult" to report)
        )
    }
}

// Use in a graph
val graph = stateGraph {
    node(prepNode)
    node(analyzeNode)      // A2A delegation
    node(reportNode)

    edge("prep", "remote-a2a-executor")
    edge("remote-a2a-executor", "report")
    startNode("prep")
}
```

## 6. Authentication

```kotlin
import io.mobilegraph.a2a.client.BearerTokenAuthProvider
import io.mobilegraph.a2a.client.ApiKeyAuthProvider

// Bearer token auth
val client = A2AClient(
    httpClient = HttpClient(),
    authProvider = BearerTokenAuthProvider("my-jwt-token")
)

// API key auth
val client = A2AClient(
    httpClient = HttpClient(),
    authProvider = ApiKeyAuthProvider(key = "my-api-key")
)
```

## 7. Configuration & Timeouts

```kotlin
import io.mobilegraph.a2a.client.A2AClientConfig

val client = A2AClient(
    httpClient = HttpClient(),
    config = A2AClientConfig(
        connectionTimeoutMs = 30_000L,   // 30 seconds
        requestTimeoutMs = 120_000L,      // 2 minutes
        socketTimeoutMs = 120_000L,       // 2 minutes inactivity read timeout
        taskPollingIntervalMs = 1_500L,
        maxRetries = 3,
        useLegacyMethodNames = false,     // false = A2A v1.0 method names
        autoFallbackToLegacy = true,      // true = auto retry v0.3 on -32601 Method Not Found
    )
)
```

## 8. Plugin Integration

For apps using the MobileGraph initialization DSL:

```kotlin
import io.mobilegraph.a2a.facade.a2a
import io.mobilegraph.a2a.client.BearerTokenAuthProvider

MobileGraph.initialize {
    a2a {
        remoteAgent("https://coding-agent.example.com") {
            authProvider = BearerTokenAuthProvider("token")
        }
        remoteAgent("https://analysis-agent.example.com") {
            authProvider = ApiKeyAuthProvider("key")
            config = A2AClientConfig(requestTimeoutMs = 120_000)
        }
    }
}
```

## 9. Error Handling

```kotlin
import io.mobilegraph.a2a.errors.*

try {
    val card = client.discoverAgent("https://agent.example.com")
    val task = client.sendMessage(card.url, params)
} catch (e: A2ADiscoveryException) {
    // Agent Card fetch/parse failed
} catch (e: A2AAuthenticationException) {
    // Authentication failed
} catch (e: A2ATaskNotFoundException) {
    // Task not found on remote agent
} catch (e: A2ATimeoutException) {
    // Operation timed out
} catch (e: A2AStreamException) {
    // SSE streaming error
} catch (e: A2AProtocolException) {
    // JSON-RPC protocol error (check e.errorCode)
} catch (e: A2ANetworkException) {
    // Transport-level failure
} catch (e: A2AException) {
    // Any A2A error
}
```

## 10. Handling Input-Required & Human-in-the-Loop (HITL)

When a remote A2A agent requires additional user input before proceeding (`task.status.state == A2ATaskState.INPUT_REQUIRED`):

### A. Graph Engine Integration (A2ANode + StateGraph)
`A2ANode` automatically maps `INPUT_REQUIRED` to `ExecutionResult.AwaitingReview`. The graph engine pauses execution and checkpoints state:

```kotlin
val result = agentRuntime.run(workflowGraph, initialState)

if (result is ExecutionResult.AwaitingReview) {
    // 1. Extract remote agent question from state
    val question = result.state.variables[A2ATaskAdapter.STATE_KEY_A2A_INPUT_REQUIRED_MESSAGE]
    println("Remote Agent Question: $question")

    // 2. Display question in UI, collect user response, and resume workflow
    val resumeResult = agentRuntime.resume(
        graph = workflowGraph,
        checkpointId = result.checkpointId ?: checkpointStore.getLatestCheckpointId(),
        nodeId = result.nodeId,
        input = mapOf("userResponse" to userFeedback),
        reExecute = true
    )
}
```

### B. Direct Client Integration
```kotlin
val task = client.sendMessage(agentCard.url, params)

if (task.status.state == A2ATaskState.INPUT_REQUIRED) {
    val question = task.status.message?.parts
        ?.filterIsInstance<TextPart>()
        ?.joinToString(" ") { it.text }

    println("Agent asks: $question")

    // Send follow-up request
    val followUp = client.sendMessage(
        agentCard.url,
        A2AMessageSendParams(
            message = A2AMessage(
                role = A2ARole.USER,
                parts = listOf(TextPart(text = "Use the main branch"))
            )
        )
    )
}
```

## 11. Multi-Modal Content

A2A supports text, files, and structured data:

```kotlin
val params = A2AMessageSendParams(
    message = A2AMessage(
        role = A2ARole.USER,
        parts = listOf(
            TextPart(text = "Review this code"),
            FilePart(
                file = FileData(
                    name = "main.kt",
                    mimeType = "text/x-kotlin",
                    bytes = Base64.encode(sourceCode.encodeToByteArray())
                )
            ),
            DataPart(
                data = buildJsonObject {
                    put("language", "kotlin")
                    put("strictMode", true)
                }
            )
        )
    )
)
```

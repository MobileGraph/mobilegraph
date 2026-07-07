# Agent Framework Guide

MobileGraph provides a powerful, graph-based Agentic framework designed for mobile devices. It enables complex multi-agent orchestration, parallel execution, human-in-the-loop workflows, and hierarchical delegation.

---

## 1. Core Concepts

### Agent
An `Agent` is a high-level entity that combines a **Brain** (ChatModel), **Logic** (StateGraph), and **Capabilities** (ToolRegistry).

- **Atomic Agent**: Performs a direct LLM call to solve a task.
- **Orchestrator Agent**: Manages a sub-workflow of other agents.

### GraphState
The "Source of Truth" for an execution. It is an immutable data structure passed from node to node.
- **`executionContext`**: Metadata like `traceId` and `requestId`.
- **`variables`**: A key-value map for business data (e.g., `"is_approved"`, `"research_results"`).
- **`userQuery`**: The original input from the user.

### Checkpointing & Persistence
MobileGraph is designed for mobile reliability. The `CheckpointStore` automatically saves the `GraphState` after every node execution.
- **Durable Execution**: If your app is killed by the OS, you can resume the agent exactly where it left off using a `checkpointId`.
- **Thread Safety**: Uses atomic locks to ensure state integrity during parallel execution.

---

### GraphNode
A `GraphNode` is a unit of work.
- `AgentNode`: Executes an `Agent`.
- `HumanReviewNode`: Pauses execution for human approval/feedback.
- `JoinNode`: Synchronizes multiple parallel branches (Fan-In).
- `EndNode`: Marks the completion of a workflow.

### GraphEdge
An `Edge` defines the transition between nodes.
- `edge(from, to)`: Standard sequential transition.
- `conditionalEdge(from, to) { state -> ... }`: Branching based on state variables.

---

## 2. Creating an Agent

To create an agent, implement the `Agent` interface:

```kotlin
class ResearchAgent(override val model: ChatModel) : Agent {
    override val name = "Researcher"
    override val description = "Gathers facts"
    override val rolePrompt = SystemMessage("You are a researcher. Provide 3 facts.")
    
    // Tools are optional
    override val tools: ToolRegistry? = null
    
    // Graphs are for Orchestrators (Sub-Agents)
    override val graph: StateGraph? = null

    override fun formatAgentInstruction(state: GraphState): UserMessage = 
        UserMessage("Research: ${state.userQuery}")

    override suspend fun handleLlmOutput(output: ModelOutput, state: GraphState): GraphState =
        state.copy(variables = state.variables + ("data" to output.asText()))
}
```

---

## 3. Building a Workflow Graph

Use the `stateGraph` DSL to define your workflow.

```kotlin
val workflow = stateGraph {
    start("researcher")
    
    node(AgentNode("researcher", researchAgent, runtime))
    node(AgentNode("writer", writerAgent, runtime))
    node(EndNode("done"))

    // Define transitions
    edge("researcher", "writer")
    edge("writer", "done")
}
```

---

## 4. Execution & Resumption

### Running a Workflow
```kotlin
val result = agentRuntime.run(workflow, initialState)
```

### Resuming from Checkpoint (Process Death / HITL)
If the workflow pauses (e.g., for Human Review) or the app restarts, use `resume`:
```kotlin
val result = agentRuntime.resume(
    graph = workflow,
    checkpointId = "trace_node_timestamp",
    nodeId = "review",
    input = mapOf("approved" to true) // Inject user feedback
)
```

---

## 5. Advanced Architectures

### Fan-Out / Fan-In (Parallel Execution)
Run multiple agents simultaneously and merge their results.

```kotlin
stateGraph {
    start("input")
    node(OrchestratorNode("input"))
    
    // Fan-out: Parallel agents
    node(AgentNode("team_a", agentA, runtime))
    node(AgentNode("team_b", agentB, runtime))

    // Fan-in: Synchronization point
    join("aggregator", incomingCount = 2) { states ->
        // Merge strategy for parallel results
        val merged = states.flatMap { it.variables.toList() }.toMap()
        states.first().copy(variables = merged)
    }

    edge("input", "team_a")
    edge("input", "team_b")
    edge("team_a", "aggregator")
    edge("team_b", "aggregator")
}
```

### Human-in-the-Loop (HITL)
Pause execution for manual intervention.

```kotlin
node(HumanReviewNode("review"))
edge("writer", "review")

// Branch based on approval
conditionalEdge("review", "publisher") { it.variables["approved"] == true }
conditionalEdge("review", "writer") { it.variables["approved"] == false }
```

### Hierarchical Sub-Agents
Agents can own their own internal graphs, allowing for "Team of Teams" orchestration.

```kotlin
class ManagerAgent(
    override val model: ChatModel,
    override val graph: StateGraph // Internal workflow managed by this agent
) : Agent {  }
```

---

## 5. Structured Output
Use `StructuredOutputParser` to get type-safe, validated data from agents.

```kotlin
@Serializable
data class TaskPlan(val tasks: List<String>)

private val parser = structuredOutputParser<TaskPlan>()

// In rolePrompt:
override val rolePrompt = SystemMessage("Plan tasks. ${parser.formatInstructions()}")

// In handleLlmOutput:
val plan = parser.parse(output) // Returns ParseResult.Success(TaskPlan)
```

---

## 6. Error Handling & Resilience
The execution engine is designed for mobile reliability:
- **Resilience**: Uses `supervisorScope` so one failed agent doesn't kill its parallel siblings.
- **Short-Circuiting**: Stops the workflow immediately if a critical error occurs.
- **Isolated Failure**: Localizes crashes to specific nodes while preserving state.

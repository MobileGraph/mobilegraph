package io.mobilegraph.agents

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.StateGraph
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.UserMessage
import io.mobilegraph.state.GraphState
import io.mobilegraph.tools.registry.DefaultToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentNodeTest {
    private class MockChatModel(
        val response: String = "mocked",
    ) : ChatModel {
        override val name = "mock"

        override fun supports(capability: io.mobilegraph.core.capability.Capability): Boolean = false

        override suspend fun invoke(
            p: ChatPromptValue,
            c: ModelConfig?,
            ctx: io.mobilegraph.core.context.ExecutionContext,
        ) = ModelOutput.ChatOutput(AssistantMessage(response))

        override fun stream(
            p: ChatPromptValue,
            c: ModelConfig?,
            ctx: io.mobilegraph.core.context.ExecutionContext,
        ): Flow<io.mobilegraph.models.ChatChunk> = emptyFlow()

        override fun readModelConfig(): ModelConfig? = null
    }

    private class MockAgent(
        override val model: ChatModel,
        override val tools: ToolRegistry? = null,
        override val graph: StateGraph? = null,
        override val useGlobalTools: Boolean = false,
    ) : Agent {
        override val name = "mock"
        override val description = "mock"
        override val rolePrompt = SystemMessage("role")

        override fun formatAgentInstruction(state: GraphState) = UserMessage("hi")

        override suspend fun handleLlmOutput(
            output: ModelOutput,
            state: GraphState,
        ): GraphState = state.copy(variables = state.variables + ("processed" to true))
    }

    private data class TestState(
        override val executionContext: io.mobilegraph.core.context.ExecutionContext =
            SimpleExecutionContext(TraceId("t"), requestId = RequestId("r")),
        override val variables: Map<String, Any?> = emptyMap(),
        override val userQuery: String = "",
    ) : GraphState {
        override fun copy(
            variables: Map<String, Any?>,
            userQuery: String,
        ): TestState = copy(executionContext = executionContext, variables = variables, userQuery = userQuery)
    }

    @Test
    fun testAtomicAgentExecution() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val runtime = DefaultAgentRuntime(io.mobilegraph.graph.DefaultExecutionEngine())
            val agent = MockAgent(MockChatModel())
            val node = AgentNode("agent", agent, runtime)

            val result = node.execute(TestState())
            assertTrue(result is ExecutionResult.Success)
            assertEquals(true, result.state.variables["processed"])
        }

    @Test
    fun testHierarchicalExecution() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val runtime = DefaultAgentRuntime(io.mobilegraph.graph.DefaultExecutionEngine())
            val subGraph =
                io.mobilegraph.graph.stateGraph {
                    start("n1")
                    node(
                        object : io.mobilegraph.graph.GraphNode {
                            override val id = "n1"

                            override suspend fun execute(state: GraphState) =
                                ExecutionResult.Success(
                                    state.copy(
                                        variables =
                                            state.variables + ("sub" to "ok"),
                                    ),
                                )
                        },
                    )
                    node(io.mobilegraph.graph.EndNode("end"))
                    edge("n1", "end")
                }
            val agent = MockAgent(MockChatModel(), graph = subGraph)
            val node = AgentNode("orchestrator", agent, runtime)

            val result = node.execute(TestState())
            assertTrue(result is ExecutionResult.Success)
            assertEquals("ok", result.state.variables["sub"])
        }

    @Test
    fun testLlmErrorHandling() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val runtime = DefaultAgentRuntime(io.mobilegraph.graph.DefaultExecutionEngine())
            val errorModel =
                object : ChatModel {
                    override val name = "error"

                    override fun supports(capability: io.mobilegraph.core.capability.Capability): Boolean = false

                    override suspend fun invoke(
                        p: ChatPromptValue,
                        c: ModelConfig?,
                        ctx: io.mobilegraph.core.context.ExecutionContext,
                    ) = ModelOutput.ErrorOutput(RuntimeException("API Error"))

                    override fun stream(
                        p: ChatPromptValue,
                        c: ModelConfig?,
                        ctx: io.mobilegraph.core.context.ExecutionContext,
                    ) = emptyFlow<io.mobilegraph.models.ChatChunk>()

                    override fun readModelConfig() = null
                }
            val agent = MockAgent(errorModel)
            val node = AgentNode("agent", agent, runtime)

            val result = node.execute(TestState())
            assertTrue(result is ExecutionResult.Error)
        }
}

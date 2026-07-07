package io.mobilegraph.agents

import io.mobilegraph.checkpoint.CheckpointId
import io.mobilegraph.checkpoint.InMemoryCheckpointStore
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.graph.DefaultExecutionEngine
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.stateGraph
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultAgentRuntimeTest {
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
    fun testRunWorkflow() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val store = InMemoryCheckpointStore()
            val engine = DefaultExecutionEngine(store)
            val runtime = DefaultAgentRuntime(engine, store)

            val graph =
                stateGraph {
                    start("start")
                    node(
                        object : io.mobilegraph.graph.GraphNode {
                            override val id = "start"

                            override suspend fun execute(state: GraphState) =
                                ExecutionResult.Success(
                                    state.copy(
                                        variables =
                                            state.variables + ("ok" to true),
                                    ),
                                )
                        },
                    )
                    node(io.mobilegraph.graph.EndNode("end"))
                    edge("start", "end")
                }

            val result = runtime.run(graph, TestState())
            assertTrue(result is ExecutionResult.Success)
            assertEquals(true, result.state.variables["ok"])
        }

    @Test
    fun testResumeWorkflow() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val store = InMemoryCheckpointStore()
            val engine = DefaultExecutionEngine(store)
            val runtime = DefaultAgentRuntime(engine, store)

            val graph =
                stateGraph {
                    start("n1")
                    node(io.mobilegraph.graph.HumanReviewNode("n1"))
                    node(io.mobilegraph.graph.EndNode("end"))
                    edge("n1", "end")
                }

            val initialState = TestState()
            val runResult = runtime.run(graph, initialState)
            assertTrue(runResult is ExecutionResult.AwaitingReview)

            // Resuming - we need to find the checkpoint ID.
            // In DefaultExecutionEngine it's currently hardcoded to end with _0
            val traceId = initialState.executionContext.traceId.value
            val checkpointId = "${traceId}_n1_0"

            val resumeResult = runtime.resume(graph, checkpointId, "n1", mapOf("approved" to true))
            assertTrue(resumeResult is ExecutionResult.Success)
            assertEquals(true, resumeResult.state.variables["approved"])
        }
}

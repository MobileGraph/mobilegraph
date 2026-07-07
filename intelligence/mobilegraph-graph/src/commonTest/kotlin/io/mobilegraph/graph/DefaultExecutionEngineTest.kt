package io.mobilegraph.graph

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultExecutionEngineTest {
    data class TestState(
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

    private class SuccessNode(
        override val id: String,
        val key: String,
        val value: String,
    ) : GraphNode {
        override suspend fun execute(state: GraphState): ExecutionResult =
            ExecutionResult.Success(state.copy(variables = state.variables + (key to value)))
    }

    @Test
    fun testSequentialExecution() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val engine = DefaultExecutionEngine()
            val graph =
                stateGraph {
                    start("n1")
                    node(SuccessNode("n1", "k1", "v1"))
                    node(SuccessNode("n2", "k2", "v2"))
                    node(EndNode("end"))

                    edge("n1", "n2")
                    edge("n2", "end")
                }

            val result = engine.execute(graph, TestState())
            assertTrue(result is ExecutionResult.Success)
            assertEquals("v1", result.state.variables["k1"])
            assertEquals("v2", result.state.variables["k2"])
        }

    @Test
    fun testParallelExecutionWithJoin() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val engine = DefaultExecutionEngine()
            val graph =
                stateGraph {
                    start("start")
                    node(HierarchicalOrchestratorNode("start"))
                    node(SuccessNode("p1", "branch1", "done"))
                    node(SuccessNode("p2", "branch2", "done"))
                    join("aggregator", incomingCount = 2) { states ->
                        val combined = mutableMapOf<String, Any?>()
                        states.forEach { combined.putAll(it.variables) }
                        states.first().copy(variables = combined)
                    }
                    node(EndNode("end"))

                    edge("start", "p1")
                    edge("start", "p2")
                    edge("p1", "aggregator")
                    edge("p2", "aggregator")
                    edge("aggregator", "end")
                }

            val result = engine.execute(graph, TestState())
            assertTrue(result is ExecutionResult.Success)
            assertEquals("done", result.state.variables["branch1"])
            assertEquals("done", result.state.variables["branch2"])
        }

    @Test
    fun testErrorResilience() =
        runTest {
            io.mobilegraph.core.facade.MobileGraph
                .initialize { }
            val engine = DefaultExecutionEngine()
            val graph =
                stateGraph {
                    start("start")
                    node(HierarchicalOrchestratorNode("start"))
                    node(
                        object : GraphNode {
                            override val id = "fail"

                            override suspend fun execute(state: GraphState) = throw RuntimeException("Boom")
                        },
                    )
                    node(SuccessNode("success", "sibling", "ok"))

                    edge("start", "fail")
                    edge("start", "success")
                }

            val result = engine.execute(graph, TestState())
            assertTrue(result is ExecutionResult.Error, "Should return Error if any branch fails")
        }
}

// Helper node for testing
class HierarchicalOrchestratorNode(
    override val id: String,
) : GraphNode {
    override suspend fun execute(state: GraphState) = ExecutionResult.Success(state)
}

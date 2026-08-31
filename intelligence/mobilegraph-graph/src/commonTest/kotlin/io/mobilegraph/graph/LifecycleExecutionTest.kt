package io.mobilegraph.graph

import io.mobilegraph.checkpoint.InMemoryCheckpointStore
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.lifecycle.LifecycleEvent
import io.mobilegraph.core.lifecycle.LifecycleRegistry
import io.mobilegraph.core.lifecycle.LifecycleState
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LifecycleExecutionTest {
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

    private class LongRunningNode(
        override val id: String,
        val duration: Long = 500,
    ) : GraphNode {
        var executeCount = 0

        override suspend fun execute(state: GraphState): ExecutionResult {
            executeCount++
            delay(duration)
            return ExecutionResult.Success(state.copy(variables = state.variables + (id to "done")))
        }
    }

    @Test
    fun testBackgroundPauseAndResume() =
        runTest {
            val registry = LifecycleRegistry()
            MobileGraph.initialize {
                component(LifecycleRegistry::class, registry)
            }

            val store = InMemoryCheckpointStore()
            val engine = DefaultExecutionEngine(store)
            val node = LongRunningNode("long_node")

            val graph =
                stateGraph {
                    start("long_node")
                    node(node)
                    node(EndNode("end"))
                    edge("long_node", "end")
                }

            // 1. Run in background, trigger pause
            launch {
                delay(100)
                registry.onEvent(LifecycleEvent.EnterBackground)
            }

            val config = ExecutionConfig(backgroundPolicy = BackgroundPolicy.PAUSE)
            val result = engine.execute(graph, TestState(), config)

            // Verify it paused
            assertTrue(result is ExecutionResult.AwaitingReview)
            assertEquals("long_node", result.nodeId)
            assertNotNull(result.checkpointId)
            assertTrue(result.checkpointId!!.contains("_auto"))

            // 2. Resume with reExecute = true
            registry.onEvent(LifecycleEvent.EnterForeground)
            val resumeResult =
                engine.resume(
                    graph = graph,
                    state = result.state,
                    nodeId = result.nodeId,
                    config = config,
                    reExecute = true,
                )

            assertTrue(resumeResult is ExecutionResult.Success)
            assertEquals("done", resumeResult.state.variables["long_node"])
            assertEquals(2, node.executeCount, "Node should have been re-executed")
        }

    @Test
    fun testResumeWithoutReExecution() =
        runTest {
            MobileGraph.initialize { }
            val engine = DefaultExecutionEngine()
            val node = LongRunningNode("node")

            val graph =
                stateGraph {
                    start("node")
                    node(node)
                    node(EndNode("end"))
                    edge("node", "end")
                }

            val state = TestState(variables = mapOf("node" to "pre-existing"))

            // Resume with reExecute = false (default behavior for HITL)
            val result =
                engine.resume(
                    graph = graph,
                    state = state,
                    nodeId = "node",
                    reExecute = false,
                )

            assertTrue(result is ExecutionResult.Success)
            assertEquals("pre-existing", result.state.variables["node"])
            assertEquals(0, node.executeCount, "Node should NOT have been executed")
        }

    @Test
    fun testBackgroundCancel() =
        runTest {
            val registry = LifecycleRegistry()
            MobileGraph.initialize {
                component(LifecycleRegistry::class, registry)
            }

            val engine = DefaultExecutionEngine()
            val node = LongRunningNode("node")

            val graph =
                stateGraph {
                    start("node")
                    node(node)
                    node(EndNode("end"))
                    edge("node", "end")
                }

            launch {
                delay(100)
                registry.onEvent(LifecycleEvent.EnterBackground)
            }

            val config = ExecutionConfig(backgroundPolicy = BackgroundPolicy.CANCEL)
            val result = engine.execute(graph, TestState(), config)

            assertTrue(result is ExecutionResult.Error, "Should return Error on BackgroundPolicy.CANCEL")
        }
}

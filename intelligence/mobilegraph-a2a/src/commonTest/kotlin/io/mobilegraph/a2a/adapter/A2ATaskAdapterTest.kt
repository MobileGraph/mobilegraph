package io.mobilegraph.a2a.adapter

import io.mobilegraph.a2a.errors.A2AValidationException
import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.A2AMessage
import io.mobilegraph.a2a.models.A2ARole
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.A2ATaskStatus
import io.mobilegraph.a2a.models.TextPart
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.state.GraphState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the A2A task adapter — the trust boundary between
 * remote A2A responses and MobileGraph graph state.
 */
class A2ATaskAdapterTest {
    private data class TestGraphState(
        override val executionContext: io.mobilegraph.core.context.ExecutionContext =
            SimpleExecutionContext(TraceId("test-trace"), requestId = RequestId("test-request")),
        override val variables: Map<String, Any?> = mapOf("existingVar" to "existingValue"),
        override val userQuery: String = "",
    ) : GraphState {
        override fun copy(
            variables: Map<String, Any?>,
            userQuery: String,
        ): TestGraphState = copy(executionContext = executionContext, variables = variables, userQuery = userQuery)
    }

    private fun createState(): GraphState = TestGraphState()

    @Test
    fun `validates task with valid ID`() {
        val task =
            A2ATask(
                id = "task-123",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )

        // Should not throw
        A2ATaskAdapter.validateTask(task)
    }

    @Test
    fun `validates task rejects empty ID`() {
        val task =
            A2ATask(
                id = "",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )

        assertFailsWith<A2AValidationException> {
            A2ATaskAdapter.validateTask(task)
        }
    }

    @Test
    fun `validates task rejects blank ID`() {
        val task =
            A2ATask(
                id = "   ",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )

        assertFailsWith<A2AValidationException> {
            A2ATaskAdapter.validateTask(task)
        }
    }

    @Test
    fun `maps completed task to state with artifacts`() {
        val task =
            A2ATask(
                id = "task-456",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
                artifacts =
                    listOf(
                        A2AArtifact(
                            artifactId = "art-1",
                            name = "Analysis Report",
                            parts =
                                listOf(
                                    TextPart(text = "Code analysis complete. All checks passed."),
                                ),
                        ),
                    ),
            )

        val state = createState()
        val result = A2ATaskAdapter.mapTaskToState(task, state, "remote-agent")

        assertEquals("task-456", result.variables[A2ATaskAdapter.STATE_KEY_A2A_TASK_ID])
        assertEquals("COMPLETED", result.variables[A2ATaskAdapter.STATE_KEY_A2A_STATUS])
        assertEquals("remote-agent", result.variables[A2ATaskAdapter.STATE_KEY_A2A_AGENT])

        val resultText = result.variables[A2ATaskAdapter.STATE_KEY_A2A_RESULT] as String
        assertTrue(resultText.contains("All checks passed"))

        // Original state preserved
        assertEquals("existingValue", result.variables["existingVar"])
    }

    @Test
    fun `maps failed task to state with error`() {
        val task =
            A2ATask(
                id = "task-fail",
                status =
                    A2ATaskStatus(
                        state = A2ATaskState.FAILED,
                        message =
                            A2AMessage(
                                role = A2ARole.AGENT,
                                parts = listOf(TextPart(text = "Permission denied")),
                            ),
                    ),
            )

        val result = A2ATaskAdapter.mapTaskToState(task, createState())

        assertEquals("FAILED", result.variables[A2ATaskAdapter.STATE_KEY_A2A_STATUS])
        assertEquals("Permission denied", result.variables[A2ATaskAdapter.STATE_KEY_A2A_ERROR])
    }

    @Test
    fun `maps input-required task to state`() {
        val task =
            A2ATask(
                id = "task-input",
                status =
                    A2ATaskStatus(
                        state = A2ATaskState.INPUT_REQUIRED,
                        message =
                            A2AMessage(
                                role = A2ARole.AGENT,
                                parts = listOf(TextPart(text = "Which branch?")),
                            ),
                    ),
            )

        val result = A2ATaskAdapter.mapTaskToState(task, createState())

        assertEquals("INPUT_REQUIRED", result.variables[A2ATaskAdapter.STATE_KEY_A2A_STATUS])
        assertEquals(
            "Which branch?",
            result.variables[A2ATaskAdapter.STATE_KEY_A2A_INPUT_REQUIRED_MESSAGE],
        )
    }

    @Test
    fun `isInputRequired correctly identifies state`() {
        val inputRequired =
            A2ATask(
                id = "t1",
                status = A2ATaskStatus(state = A2ATaskState.INPUT_REQUIRED),
            )
        val completed =
            A2ATask(
                id = "t2",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )

        assertTrue(A2ATaskAdapter.isInputRequired(inputRequired))
        assertFalse(A2ATaskAdapter.isInputRequired(completed))
    }

    @Test
    fun `isTerminal correctly identifies terminal states`() {
        val completed =
            A2ATask(
                id = "t1",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )
        val working =
            A2ATask(
                id = "t2",
                status = A2ATaskStatus(state = A2ATaskState.WORKING),
            )

        assertTrue(A2ATaskAdapter.isTerminal(completed))
        assertFalse(A2ATaskAdapter.isTerminal(working))
    }

    @Test
    fun `maps task with multiple artifacts`() {
        val task =
            A2ATask(
                id = "multi-art",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
                artifacts =
                    listOf(
                        A2AArtifact(
                            artifactId = "art-1",
                            name = "Summary",
                            parts = listOf(TextPart(text = "Part 1 text")),
                        ),
                        A2AArtifact(
                            artifactId = "art-2",
                            name = "Details",
                            parts = listOf(TextPart(text = "Part 2 text")),
                        ),
                    ),
            )

        val result = A2ATaskAdapter.mapTaskToState(task, createState())

        val resultText = result.variables[A2ATaskAdapter.STATE_KEY_A2A_RESULT] as String
        assertTrue(resultText.contains("Part 1 text"))
        assertTrue(resultText.contains("Part 2 text"))

        @Suppress("UNCHECKED_CAST")
        val artifacts = result.variables[A2ATaskAdapter.STATE_KEY_A2A_ARTIFACTS] as List<Map<String, Any?>>
        assertEquals(2, artifacts.size)
    }

    @Test
    fun `maps task without agent name`() {
        val task =
            A2ATask(
                id = "no-agent",
                status = A2ATaskStatus(state = A2ATaskState.COMPLETED),
            )

        val result = A2ATaskAdapter.mapTaskToState(task, createState())

        assertFalse(result.variables.containsKey(A2ATaskAdapter.STATE_KEY_A2A_AGENT))
    }

    @Test
    fun `maps task with A2A v1_0 Protobuf status state and message parts without type discriminator`() {
        val task =
            A2ATask(
                id = "77bf3c16-de01-4c8c-b50d-60bc81c9f7c4",
                contextId = "bfe071a6-919a-4562-88a0-6663b8dcf4da",
                status =
                    A2ATaskStatus(
                        state = A2ATaskState.COMPLETED,
                        message =
                            A2AMessage(
                                role = A2ARole.AGENT,
                                parts = listOf(TextPart(text = "⭐ Hey there! Working late tonight?")),
                            ),
                    ),
                artifacts =
                    listOf(
                        A2AArtifact(
                            artifactId = "2f511e01-42cc-437f-92ef-19dfd5de8a3c",
                            name = "response",
                            parts = listOf(TextPart(text = "⭐ Hey there! Working late tonight?")),
                        ),
                    ),
            )

        val result = A2ATaskAdapter.mapTaskToState(task, createState(), "A2A Multi-Skill Test Server")

        assertEquals("77bf3c16-de01-4c8c-b50d-60bc81c9f7c4", result.variables[A2ATaskAdapter.STATE_KEY_A2A_TASK_ID])
        assertEquals("COMPLETED", result.variables[A2ATaskAdapter.STATE_KEY_A2A_STATUS])
        val resultText = result.variables[A2ATaskAdapter.STATE_KEY_A2A_RESULT] as String
        assertTrue(resultText.contains("Hey there! Working late tonight?"))
    }
}

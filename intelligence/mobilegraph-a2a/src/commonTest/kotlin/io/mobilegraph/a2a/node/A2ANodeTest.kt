package io.mobilegraph.a2a.node

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.a2a.client.A2AClient
import io.mobilegraph.a2a.models.A2AMessage
import io.mobilegraph.a2a.models.A2ARole
import io.mobilegraph.a2a.models.TextPart
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.state.GraphState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class A2ANodeTest {
    private data class TestGraphState(
        override val executionContext: io.mobilegraph.core.context.ExecutionContext =
            SimpleExecutionContext(TraceId("node-trace"), requestId = RequestId("node-req")),
        override val variables: Map<String, Any?> = mapOf("query" to "Analyze security in main.kt"),
        override val userQuery: String = "Analyze security in main.kt",
    ) : GraphState {
        override fun copy(
            variables: Map<String, Any?>,
            userQuery: String,
        ): TestGraphState = copy(executionContext = executionContext, variables = variables, userQuery = userQuery)
    }

    @Test
    fun `executes synchronous node successfully`() =
        runTest {
            val completedTaskJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-node-1",
                        "status": {
                            "state": "completed"
                        },
                        "artifacts": [
                            {
                                "artifactId": "art-1",
                                "name": "Security Report",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "No vulnerabilities found."
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = completedTaskJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                a2aNode("security-checker") {
                    this.client = client
                    this.agentUrl = "https://agent.example.com/a2a"
                    textMessage { state ->
                        state.variables["query"] as String
                    }
                }

            val result = node.execute(TestGraphState())

            assertTrue(result is ExecutionResult.Success)
            val resultText = result.state.variables["a2a_result"] as String
            assertTrue(resultText.contains("No vulnerabilities found"))

            client.close()
        }

    @Test
    fun `executes node with input-required returning AwaitingReview`() =
        runTest {
            val inputRequiredJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-node-2",
                        "status": {
                            "state": "input-required",
                            "message": {
                                "role": "agent",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Provide repository access token."
                                    }
                                ]
                            }
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = inputRequiredJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                A2ANode(
                    id = "security-checker",
                    client = client,
                    agentUrl = "https://agent.example.com/a2a",
                    messageBuilder = { state ->
                        A2AMessage(
                            role = A2ARole.USER,
                            parts = listOf(TextPart(state.userQuery)),
                        )
                    },
                )

            val result = node.execute(TestGraphState())

            assertTrue(result is ExecutionResult.AwaitingReview)
            assertEquals("security-checker", result.nodeId)
            val msg = result.state.variables["a2a_input_required_message"] as String
            assertTrue(msg.contains("Provide repository access token"))

            client.close()
        }

    @Test
    fun `executes node with task failure returning Error`() =
        runTest {
            val failedTaskJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-node-3",
                        "status": {
                            "state": "failed",
                            "message": {
                                "role": "agent",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Model quota exceeded."
                                    }
                                ]
                            }
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = failedTaskJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                a2aNode("security-checker") {
                    this.client = client
                    this.agentUrl = "https://agent.example.com/a2a"
                    textMessage { "Scan repo" }
                }

            val result = node.execute(TestGraphState())

            assertTrue(result is ExecutionResult.Error)
            val err = result.state.variables["a2a_error"] as String
            assertTrue(err.contains("Model quota exceeded"))

            client.close()
        }

    @Test
    fun `custom result mapper maps state accurately`() =
        runTest {
            val completedTaskJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-node-4",
                        "status": {
                            "state": "completed"
                        },
                        "artifacts": [
                            {
                                "artifactId": "art-1",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Report content"
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = completedTaskJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                a2aNode("custom-mapped-node") {
                    this.client = client
                    this.agentUrl = "https://agent.example.com/a2a"
                    textMessage { "Test" }
                    mapResult { task, state ->
                        state.copy(
                            variables = state.variables + ("custom_key" to "custom_value_${task.id}"),
                        )
                    }
                }

            val result = node.execute(TestGraphState())

            assertTrue(result is ExecutionResult.Success)
            assertEquals("custom_value_task-node-4", result.state.variables["custom_key"])

            client.close()
        }

    @Test
    fun `streaming node automatically falls back to synchronous execution on stream method not found -32601 error`() =
        runTest {
            val methodNotFoundJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "error": {
                        "code": -32601,
                        "message": "Method not found"
                    }
                }
                """.trimIndent()

            val syncSuccessJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-fallback-sync-01",
                        "status": {
                            "state": "completed"
                        },
                        "artifacts": [
                            {
                                "artifactId": "art-1",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Fallback sync result"
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    val requestText = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    if (requestText.contains("StreamMessage") || requestText.contains("message/stream")) {
                        respond(
                            content = methodNotFoundJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content = syncSuccessJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                a2aNode("streaming-fallback-node") {
                    this.client = client
                    this.agentUrl = "https://agent.example.com/a2a"
                    this.streaming = true
                    textMessage { "Test query" }
                }

            val result = node.execute(TestGraphState())

            assertTrue(result is ExecutionResult.Success)
            val resultText = result.state.variables["a2a_result"] as String
            assertTrue(resultText.contains("Fallback sync result"))

            client.close()
        }

    @Test
    fun `node auto attaches previous taskId and contextId for HITL follow up messages`() =
        runTest {
            var capturedRequestText = ""
            val hitlApprovedResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "9a0e5887-31e9-4b36-aa3c-c5d901dd7b95",
                        "contextId": "6e10e889-c5e3-4a32-b98b-298a150b8367",
                        "status": {
                            "state": "completed"
                        },
                        "artifacts": [
                            {
                                "artifactId": "art-approved-1",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Transfer $500 to Alice approved and processed."
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    capturedRequestText = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    respond(
                        content = hitlApprovedResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val node =
                a2aNode("hitl-resume-node") {
                    this.client = client
                    this.agentUrl = "https://agent.example.com/a2a"
                    textMessage { state ->
                        state.variables["userResponse"] as? String ?: state.userQuery
                    }
                }

            val resumedState =
                TestGraphState(
                    variables =
                        mapOf(
                            "a2a_task_id" to "9a0e5887-31e9-4b36-aa3c-c5d901dd7b95",
                            "a2a_context_id" to "6e10e889-c5e3-4a32-b98b-298a150b8367",
                            "userResponse" to "yes",
                        ),
                    userQuery = "/hitl transfer $500 to Alice",
                )

            val result = node.execute(resumedState)

            assertTrue(result is ExecutionResult.Success)
            assertTrue(capturedRequestText.contains(""""taskId":"9a0e5887-31e9-4b36-aa3c-c5d901dd7b95""""))
            assertTrue(capturedRequestText.contains(""""contextId":"6e10e889-c5e3-4a32-b98b-298a150b8367""""))
            assertTrue(capturedRequestText.contains(""""text":"yes""""))

            client.close()
        }
}

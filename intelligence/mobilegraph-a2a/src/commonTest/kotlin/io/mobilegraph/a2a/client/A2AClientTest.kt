package io.mobilegraph.a2a.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.a2a.errors.A2ADiscoveryException
import io.mobilegraph.a2a.errors.A2AProtocolException
import io.mobilegraph.a2a.errors.A2ATaskNotFoundException
import io.mobilegraph.a2a.errors.A2AValidationException
import io.mobilegraph.a2a.models.A2AMessage
import io.mobilegraph.a2a.models.A2AMessageSendParams
import io.mobilegraph.a2a.models.A2ARole
import io.mobilegraph.a2a.models.A2ATaskCancelParams
import io.mobilegraph.a2a.models.A2ATaskQueryParams
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.TextPart
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the A2A client using Ktor mock engine.
 * Verifies wire-level JSON-RPC compatibility.
 */
class A2AClientTest {
    private val agentCardJson =
        """
        {
            "name": "Test Agent",
            "description": "A test A2A agent",
            "url": "https://test-agent.example.com/a2a",
            "version": "1.0.0",
            "protocolVersion": "0.2.1",
            "capabilities": {
                "streaming": true
            },
            "skills": [
                {
                    "id": "test-skill",
                    "name": "Test Skill",
                    "description": "A test skill"
                }
            ]
        }
        """.trimIndent()

    @Test
    fun `discover agent fetches and parses Agent Card`() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertTrue(request.url.encodedPath.endsWith("/.well-known/agent-card.json"))
                    respond(
                        content = agentCardJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val card = client.discoverAgent("https://test-agent.example.com")

            assertEquals("Test Agent", card.name)
            assertEquals("https://test-agent.example.com/a2a", card.url)
            assertEquals("0.2.1", card.protocolVersion)
            assertEquals(1, card.skills.size)
            assertEquals("test-skill", card.skills[0].id)

            client.close()
        }

    @Test
    fun `discover agent caches result`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine { _ ->
                    requestCount++
                    respond(
                        content = agentCardJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            client.discoverAgent("https://test-agent.example.com")
            client.discoverAgent("https://test-agent.example.com")

            assertEquals(1, requestCount)

            client.close()
        }

    @Test
    fun `discover agent force refresh bypasses cache`() =
        runTest {
            var requestCount = 0
            val mockEngine =
                MockEngine { _ ->
                    requestCount++
                    respond(
                        content = agentCardJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            client.discoverAgent("https://test-agent.example.com")
            client.discoverAgent("https://test-agent.example.com", forceRefresh = true)

            assertEquals(2, requestCount)

            client.close()
        }

    @Test
    fun `discover agent throws on HTTP error`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respondBadRequest()
                }

            val client = A2AClient(HttpClient(mockEngine))

            assertFailsWith<A2ADiscoveryException> {
                client.discoverAgent("https://bad-agent.example.com")
            }

            client.close()
        }

    @Test
    fun `discover agent throws on invalid JSON`() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = "not json",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))

            assertFailsWith<A2ADiscoveryException> {
                client.discoverAgent("https://bad-json.example.com")
            }

            client.close()
        }

    @Test
    fun `discover agent validates required fields`() =
        runTest {
            val invalidCardJson = """{"name": "", "url": ""}"""
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = invalidCardJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))

            assertFailsWith<A2AValidationException> {
                client.discoverAgent("https://invalid-card.example.com")
            }

            client.close()
        }

    @Test
    fun `sendMessage sends JSON-RPC request and returns task`() =
        runTest {
            val taskResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-001",
                        "status": {
                            "state": "completed",
                            "timestamp": "2025-06-01T12:00:00Z"
                        },
                        "artifacts": [
                            {
                                "artifactId": "art-1",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Analysis complete. No issues found."
                                    }
                                ]
                            }
                        ]
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { request ->
                    // Verify it's a POST with JSON-RPC content
                    assertEquals("application/json", request.body.contentType?.toString())

                    respond(
                        content = taskResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val task =
                client.sendMessage(
                    "https://test-agent.example.com/a2a",
                    A2AMessageSendParams(
                        message =
                            A2AMessage(
                                role = A2ARole.USER,
                                parts = listOf(TextPart(text = "Analyze this code")),
                            ),
                    ),
                )

            assertEquals("task-001", task.id)
            assertEquals(A2ATaskState.COMPLETED, task.status.state)
            assertEquals(1, task.artifacts.size)
            val textPart = task.artifacts[0].parts[0] as TextPart
            assertTrue(textPart.text.contains("No issues found"))

            client.close()
        }

    @Test
    fun `getTask retrieves task status`() =
        runTest {
            val taskResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-002",
                        "status": {
                            "state": "working"
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = taskResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val task =
                client.getTask(
                    "https://test-agent.example.com/a2a",
                    A2ATaskQueryParams(id = "task-002"),
                )

            assertEquals("task-002", task.id)
            assertEquals(A2ATaskState.WORKING, task.status.state)

            client.close()
        }

    @Test
    fun `cancelTask cancels a task`() =
        runTest {
            val cancelResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-003",
                        "status": {
                            "state": "canceled"
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = cancelResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val task =
                client.cancelTask(
                    "https://test-agent.example.com/a2a",
                    A2ATaskCancelParams(id = "task-003"),
                )

            assertEquals("task-003", task.id)
            assertEquals(A2ATaskState.CANCELED, task.status.state)

            client.close()
        }

    @Test
    fun `sendMessage throws on JSON-RPC error response`() =
        runTest {
            val errorResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "error": {
                        "code": -32603,
                        "message": "Internal server error"
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = errorResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))

            val exception =
                assertFailsWith<A2AProtocolException> {
                    client.sendMessage(
                        "https://test-agent.example.com/a2a",
                        A2AMessageSendParams(
                            message =
                                A2AMessage(
                                    role = A2ARole.USER,
                                    parts = listOf(TextPart(text = "test")),
                                ),
                        ),
                    )
                }

            assertTrue(exception.message!!.contains("Internal server error"))

            client.close()
        }

    @Test
    fun `getTask throws A2ATaskNotFoundException for task not found`() =
        runTest {
            val notFoundResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "error": {
                        "code": -32001,
                        "message": "Task not found"
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = notFoundResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))

            assertFailsWith<A2ATaskNotFoundException> {
                client.getTask(
                    "https://test-agent.example.com/a2a",
                    A2ATaskQueryParams(id = "nonexistent-task"),
                )
            }

            client.close()
        }

    @Test
    fun `sendMessage handles input-required state`() =
        runTest {
            val inputRequiredJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-input",
                        "status": {
                            "state": "input-required",
                            "message": {
                                "role": "agent",
                                "parts": [
                                    {
                                        "type": "text",
                                        "text": "Which branch should I analyze?"
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
            val task =
                client.sendMessage(
                    "https://test-agent.example.com/a2a",
                    A2AMessageSendParams(
                        message =
                            A2AMessage(
                                role = A2ARole.USER,
                                parts = listOf(TextPart(text = "Analyze repo")),
                            ),
                    ),
                )

            assertEquals(A2ATaskState.INPUT_REQUIRED, task.status.state)
            assertNotNull(task.status.message)

            client.close()
        }

    @Test
    fun `bearer token auth provider adds authorization header`() =
        runTest {
            var capturedAuthHeader: String? = null
            val mockEngine =
                MockEngine { request ->
                    capturedAuthHeader = request.headers["Authorization"]
                    respond(
                        content = """{"jsonrpc":"2.0","id":1,"result":{"id":"t1","status":{"state":"completed"}}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                A2AClient(
                    httpClient = HttpClient(mockEngine),
                    authProvider = BearerTokenAuthProvider("my-secret-token"),
                )

            client.sendMessage(
                "https://test-agent.example.com/a2a",
                A2AMessageSendParams(
                    message =
                        A2AMessage(
                            role = A2ARole.USER,
                            parts = listOf(TextPart(text = "test")),
                        ),
                ),
            )

            assertEquals("Bearer my-secret-token", capturedAuthHeader)

            client.close()
        }

    @Test
    fun `sendMessage automatically falls back to legacy v0_3 method on method not found -32601 error`() =
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

            val taskCompletedJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "id": "task-fallback-01",
                        "status": {
                            "state": "completed"
                        }
                    }
                }
                """.trimIndent()

            val capturedMethods = mutableListOf<String>()

            val mockEngine =
                MockEngine { request ->
                    val requestText = (request.body as io.ktor.http.content.OutgoingContent.ByteArrayContent).bytes().decodeToString()
                    val method =
                        if (requestText.contains("SendMessage")) {
                            "SendMessage"
                        } else if (requestText.contains("message/send")) {
                            "message/send"
                        } else {
                            "unknown"
                        }
                    capturedMethods.add(method)

                    if (method == "SendMessage") {
                        respond(
                            content = methodNotFoundJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content = taskCompletedJson,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }

            val client = A2AClient(HttpClient(mockEngine))
            val task =
                client.sendMessage(
                    "https://test-agent.example.com/a2a",
                    A2AMessageSendParams(
                        message =
                            A2AMessage(
                                role = A2ARole.USER,
                                parts = listOf(TextPart(text = "Hello")),
                            ),
                    ),
                )

            assertEquals("task-fallback-01", task.id)
            assertEquals(2, capturedMethods.size)
            assertEquals("SendMessage", capturedMethods[0])
            assertEquals("message/send", capturedMethods[1])

            client.close()
        }

    @Test
    fun `sendMessage parses wrapped task object in A2A v1_0 response`() =
        runTest {
            val wrappedResponseJson =
                """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "result": {
                        "task": {
                            "id": "task-wrapped-99",
                            "status": {
                                "state": "completed"
                            },
                            "artifacts": [
                                {
                                    "artifactId": "art-99",
                                    "parts": [
                                        {
                                            "type": "text",
                                            "text": "Wrapped result"
                                        }
                                    ]
                                }
                            ]
                        }
                    }
                }
                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = wrappedResponseJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val task =
                client.sendMessage(
                    "https://test-agent.example.com/a2a",
                    A2AMessageSendParams(
                        message =
                            A2AMessage(
                                role = A2ARole.USER,
                                parts = listOf(TextPart(text = "Greet")),
                            ),
                    ),
                )

            assertEquals("task-wrapped-99", task.id)
            assertEquals(A2ATaskState.COMPLETED, task.status.state)
            assertEquals(1, task.artifacts.size)

            client.close()
        }

    @Test
    fun `streamMessage parses statusUpdate SSE events in A2A v1_0 format`() =
        runTest {
            val sseData =
                """
                data: {"result": {"statusUpdate": {"taskId": "0fa22222-665f-480e-bf7b-010d80c89622", "contextId": "ctx-1", "status": {"state": "TASK_STATE_WORKING", "message": {"role": "ROLE_AGENT", "parts": [{"text": "Processing with OpenAISkill..."}]}}}}, "id": "stream-test", "jsonrpc": "2.0"}

                """.trimIndent()

            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = sseData,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/event-stream"),
                    )
                }

            val client = A2AClient(HttpClient(mockEngine))
            val events = mutableListOf<A2AStreamEvent>()

            client
                .streamMessage(
                    "https://test-agent.example.com/a2a",
                    A2AMessageSendParams(
                        message =
                            A2AMessage(
                                role = A2ARole.USER,
                                parts = listOf(TextPart(text = "test")),
                            ),
                    ),
                ).collect { events.add(it) }

            assertEquals(1, events.size)
            assertTrue(events[0] is A2AStreamEvent.TaskStatusUpdate)
            val update = events[0] as A2AStreamEvent.TaskStatusUpdate
            assertEquals("0fa22222-665f-480e-bf7b-010d80c89622", update.taskId)
            assertEquals(A2ATaskState.WORKING, update.status.state)

            client.close()
        }
}

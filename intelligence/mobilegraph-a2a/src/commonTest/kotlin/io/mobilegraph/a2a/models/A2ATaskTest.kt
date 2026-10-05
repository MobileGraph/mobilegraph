package io.mobilegraph.a2a.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for A2A task model serialization and state management.
 */
class A2ATaskTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Test
    fun `deserialize task with submitted status`() {
        val taskJson =
            """
            {
                "id": "task-123",
                "contextId": "ctx-456",
                "status": {
                    "state": "submitted",
                    "timestamp": "2025-06-01T12:00:00Z"
                },
                "history": [],
                "artifacts": []
            }
            """.trimIndent()

        val task = json.decodeFromString<A2ATask>(taskJson)
        assertEquals("task-123", task.id)
        assertEquals("ctx-456", task.contextId)
        assertEquals(A2ATaskState.SUBMITTED, task.status.state)
        assertFalse(task.status.state.isTerminal())
    }

    @Test
    fun `deserialize task with completed status and artifacts`() {
        val taskJson =
            """
            {
                "id": "task-789",
                "status": {
                    "state": "completed",
                    "timestamp": "2025-06-01T12:05:00Z"
                },
                "artifacts": [
                    {
                        "artifactId": "artifact-1",
                        "name": "Code Review",
                        "description": "Review results",
                        "parts": [
                            {
                                "type": "text",
                                "text": "Found 3 issues:\n1. Missing null check\n2. Unused import\n3. Inefficient loop"
                            }
                        ],
                        "index": 0
                    }
                ]
            }
            """.trimIndent()

        val task = json.decodeFromString<A2ATask>(taskJson)
        assertEquals("task-789", task.id)
        assertEquals(A2ATaskState.COMPLETED, task.status.state)
        assertTrue(task.status.state.isTerminal())
        assertEquals(1, task.artifacts.size)
        assertEquals("artifact-1", task.artifacts[0].artifactId)
        assertEquals("Code Review", task.artifacts[0].name)

        val textPart = task.artifacts[0].parts[0] as TextPart
        assertTrue(textPart.text.contains("3 issues"))
    }

    @Test
    fun `deserialize task with input-required status`() {
        val taskJson =
            """
            {
                "id": "task-input",
                "status": {
                    "state": "input-required",
                    "message": {
                        "role": "agent",
                        "parts": [
                            {
                                "type": "text",
                                "text": "Please provide the repository URL to analyze."
                            }
                        ]
                    }
                }
            }
            """.trimIndent()

        val task = json.decodeFromString<A2ATask>(taskJson)
        assertEquals(A2ATaskState.INPUT_REQUIRED, task.status.state)
        assertFalse(task.status.state.isTerminal())
        val statusMessage = task.status.message
        assertEquals(A2ARole.AGENT, statusMessage?.role)
        val textPart = statusMessage?.parts?.get(0) as TextPart
        assertTrue(textPart.text.contains("repository URL"))
    }

    @Test
    fun `deserialize task with failed status`() {
        val taskJson =
            """
            {
                "id": "task-fail",
                "status": {
                    "state": "failed",
                    "message": {
                        "role": "agent",
                        "parts": [
                            {
                                "type": "text",
                                "text": "Unable to access the repository. Permission denied."
                            }
                        ]
                    }
                }
            }
            """.trimIndent()

        val task = json.decodeFromString<A2ATask>(taskJson)
        assertEquals(A2ATaskState.FAILED, task.status.state)
        assertTrue(task.status.state.isTerminal())
    }

    @Test
    fun `all terminal states are correctly identified`() {
        assertTrue(A2ATaskState.COMPLETED.isTerminal())
        assertTrue(A2ATaskState.FAILED.isTerminal())
        assertTrue(A2ATaskState.CANCELED.isTerminal())
        assertFalse(A2ATaskState.SUBMITTED.isTerminal())
        assertFalse(A2ATaskState.WORKING.isTerminal())
        assertFalse(A2ATaskState.INPUT_REQUIRED.isTerminal())
    }

    @Test
    fun `deserialize message with multiple part types`() {
        val messageJson =
            """
            {
                "role": "user",
                "parts": [
                    {
                        "type": "text",
                        "text": "Please analyze this file:"
                    },
                    {
                        "type": "file",
                        "file": {
                            "name": "main.py",
                            "mimeType": "text/x-python",
                            "bytes": "cHJpbnQoJ2hlbGxvJyk="
                        }
                    },
                    {
                        "type": "data",
                        "data": {"config": {"lint": true, "format": true}}
                    }
                ]
            }
            """.trimIndent()

        val message = json.decodeFromString<A2AMessage>(messageJson)
        assertEquals(A2ARole.USER, message.role)
        assertEquals(3, message.parts.size)

        assertTrue(message.parts[0] is TextPart)
        assertEquals("Please analyze this file:", (message.parts[0] as TextPart).text)

        assertTrue(message.parts[1] is FilePart)
        val filePart = message.parts[1] as FilePart
        assertEquals("main.py", filePart.file.name)
        assertEquals("text/x-python", filePart.file.mimeType)

        assertTrue(message.parts[2] is DataPart)
    }

    @Test
    fun `serialize message send params`() {
        val params =
            A2AMessageSendParams(
                message =
                    A2AMessage(
                        role = A2ARole.USER,
                        parts =
                            listOf(
                                TextPart(text = "Analyze this repository"),
                            ),
                    ),
                configuration =
                    A2AMessageSendConfiguration(
                        historyLength = 10,
                        acceptedOutputModes = listOf("text/plain", "application/json"),
                    ),
            )

        val serialized = json.encodeToString(params)
        val deserialized = json.decodeFromString<A2AMessageSendParams>(serialized)

        assertEquals(A2ARole.USER, deserialized.message.role)
        assertEquals(1, deserialized.message.parts.size)
        assertEquals(10, deserialized.configuration?.historyLength)
    }

    @Test
    fun `serialize task query params`() {
        val params =
            A2ATaskQueryParams(
                id = "task-123",
                historyLength = 5,
            )

        val serialized = json.encodeToString(params)
        val deserialized = json.decodeFromString<A2ATaskQueryParams>(serialized)

        assertEquals("task-123", deserialized.id)
        assertEquals(5, deserialized.historyLength)
    }

    @Test
    fun `serialize task cancel params`() {
        val params =
            A2ATaskCancelParams(
                id = "task-456",
            )

        val serialized = json.encodeToString(params)
        val deserialized = json.decodeFromString<A2ATaskCancelParams>(serialized)

        assertEquals("task-456", deserialized.id)
    }

    @Test
    fun `deserialize artifact with streaming metadata`() {
        val artifactJson =
            """
            {
                "artifactId": "art-1",
                "name": "Report",
                "parts": [
                    {
                        "type": "text",
                        "text": "Partial result..."
                    }
                ],
                "index": 0,
                "append": true,
                "lastChunk": false
            }
            """.trimIndent()

        val artifact = json.decodeFromString<A2AArtifact>(artifactJson)
        assertEquals("art-1", artifact.artifactId)
        assertEquals(true, artifact.append)
        assertEquals(false, artifact.lastChunk)
        assertEquals(0, artifact.index)
    }

    @Test
    fun `deserialize file part with URI reference`() {
        val partJson =
            """
            {
                "type": "file",
                "file": {
                    "name": "report.pdf",
                    "mimeType": "application/pdf",
                    "uri": "https://storage.example.com/reports/report.pdf"
                }
            }
            """.trimIndent()

        val part = json.decodeFromString<A2APart>(partJson)
        assertTrue(part is FilePart)
        val filePart = part as FilePart
        assertEquals("report.pdf", filePart.file.name)
        assertEquals("application/pdf", filePart.file.mimeType)
        assertEquals("https://storage.example.com/reports/report.pdf", filePart.file.uri)
    }

    @Test
    fun `serialize and deserialize A2ARole across v1_0 uppercase and legacy lowercase`() {
        val userMessage = A2AMessage(role = A2ARole.USER, parts = listOf(TextPart(text = "hello")))
        val serializedUser = json.encodeToString(userMessage)
        assertTrue(serializedUser.contains(""""role":"USER""""))

        val agentMessage = A2AMessage(role = A2ARole.AGENT, parts = listOf(TextPart(text = "hi")))
        val serializedAgent = json.encodeToString(agentMessage)
        assertTrue(serializedAgent.contains(""""role":"AGENT""""))

        val deserializedFromLowercase = json.decodeFromString<A2AMessage>("""{"role":"user","parts":[]}""")
        assertEquals(A2ARole.USER, deserializedFromLowercase.role)

        val deserializedFromUppercase = json.decodeFromString<A2AMessage>("""{"role":"AGENT","parts":[]}""")
        assertEquals(A2ARole.AGENT, deserializedFromUppercase.role)

        val deserializedFromRolePrefix = json.decodeFromString<A2AMessage>("""{"role":"ROLE_USER","parts":[]}""")
        assertEquals(A2ARole.USER, deserializedFromRolePrefix.role)
    }

    @Test
    fun `deserialize A2ATaskState with TASK_STATE_ prefix correctly`() {
        val taskSubmitted = json.decodeFromString<A2ATask>("""{"id":"t1","status":{"state":"TASK_STATE_SUBMITTED"}}""")
        assertEquals(A2ATaskState.SUBMITTED, taskSubmitted.status.state)

        val taskWorking = json.decodeFromString<A2ATask>("""{"id":"t2","status":{"state":"TASK_STATE_WORKING"}}""")
        assertEquals(A2ATaskState.WORKING, taskWorking.status.state)

        val taskCompleted = json.decodeFromString<A2ATask>("""{"id":"t3","status":{"state":"TASK_STATE_COMPLETED"}}""")
        assertEquals(A2ATaskState.COMPLETED, taskCompleted.status.state)

        val taskFailed = json.decodeFromString<A2ATask>("""{"id":"t4","status":{"state":"TASK_STATE_FAILED"}}""")
        assertEquals(A2ATaskState.FAILED, taskFailed.status.state)

        val taskInputRequired = json.decodeFromString<A2ATask>("""{"id":"t5","status":{"state":"TASK_STATE_INPUT_REQUIRED"}}""")
        assertEquals(A2ATaskState.INPUT_REQUIRED, taskInputRequired.status.state)
    }
}

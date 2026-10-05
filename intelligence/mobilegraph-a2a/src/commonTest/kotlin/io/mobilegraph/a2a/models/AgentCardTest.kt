package io.mobilegraph.a2a.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Agent Card serialization, deserialization, and validation.
 */
class AgentCardTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Test
    fun `deserialize minimal Agent Card`() {
        val cardJson =
            """
            {
                "name": "Test Agent",
                "url": "https://agent.example.com/a2a"
            }
            """.trimIndent()

        val card = json.decodeFromString<AgentCard>(cardJson)
        assertEquals("Test Agent", card.name)
        assertEquals("https://agent.example.com/a2a", card.url)
        assertTrue(card.skills.isEmpty())
    }

    @Test
    fun `deserialize full Agent Card`() {
        val cardJson =
            """
            {
                "name": "Coding Agent",
                "description": "An agent that helps with coding tasks",
                "url": "https://coding-agent.example.com/a2a",
                "version": "1.0.0",
                "protocolVersion": "0.2.1",
                "capabilities": {
                    "streaming": true,
                    "pushNotifications": false,
                    "stateTransitionHistory": true
                },
                "skills": [
                    {
                        "id": "code-review",
                        "name": "Code Review",
                        "description": "Reviews code for issues and improvements",
                        "tags": ["code", "review", "quality"],
                        "examples": ["Review this Python function", "Find bugs in this code"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain", "application/json"]
                    },
                    {
                        "id": "bug-fix",
                        "name": "Bug Fix",
                        "description": "Identifies and fixes bugs in code",
                        "tags": ["code", "fix", "debug"],
                        "examples": ["Fix the null pointer exception"],
                        "inputModes": ["text/plain"],
                        "outputModes": ["text/plain"]
                    }
                ],
                "securitySchemes": {
                    "bearerAuth": {
                        "type": "http",
                        "scheme": "bearer",
                        "bearerFormat": "JWT"
                    }
                },
                "security": [{"bearerAuth": []}],
                "defaultInputModes": ["text/plain"],
                "defaultOutputModes": ["text/plain", "application/json"],
                "provider": {
                    "organization": "Example Corp",
                    "url": "https://example.com"
                },
                "documentationUrl": "https://docs.example.com/coding-agent"
            }
            """.trimIndent()

        val card = json.decodeFromString<AgentCard>(cardJson)
        assertEquals("Coding Agent", card.name)
        assertEquals("An agent that helps with coding tasks", card.description)
        assertEquals("https://coding-agent.example.com/a2a", card.url)
        assertEquals("1.0.0", card.version)
        assertEquals("0.2.1", card.protocolVersion)

        // Capabilities
        assertNotNull(card.capabilities)
        assertEquals(true, card.capabilities!!.streaming)
        assertEquals(false, card.capabilities!!.pushNotifications)
        assertEquals(true, card.capabilities!!.stateTransitionHistory)

        // Skills
        assertEquals(2, card.skills.size)
        assertEquals("code-review", card.skills[0].id)
        assertEquals("Code Review", card.skills[0].name)
        assertEquals(3, card.skills[0].tags.size)
        assertEquals(2, card.skills[0].examples.size)

        // Security
        assertEquals(1, card.securitySchemes.size)
        val bearerAuth = card.securitySchemes["bearerAuth"]
        assertNotNull(bearerAuth)
        assertEquals("http", bearerAuth.type)
        assertEquals("bearer", bearerAuth.scheme)
        assertEquals("JWT", bearerAuth.bearerFormat)

        // Provider
        assertNotNull(card.provider)
        assertEquals("Example Corp", card.provider!!.organization)
    }

    @Test
    fun `serialize and deserialize Agent Card roundtrip`() {
        val card =
            AgentCard(
                name = "My Agent",
                description = "A test agent",
                url = "https://my-agent.example.com/a2a",
                version = "2.0.0",
                protocolVersion = "0.2.1",
                capabilities =
                    AgentCardCapabilities(
                        streaming = true,
                        pushNotifications = false,
                    ),
                skills =
                    listOf(
                        AgentSkill(
                            id = "analyze",
                            name = "Analyze",
                            description = "Analyzes things",
                            tags = listOf("analysis"),
                        ),
                    ),
            )

        val serialized = json.encodeToString(card)
        val deserialized = json.decodeFromString<AgentCard>(serialized)

        assertEquals(card.name, deserialized.name)
        assertEquals(card.description, deserialized.description)
        assertEquals(card.url, deserialized.url)
        assertEquals(card.version, deserialized.version)
        assertEquals(card.protocolVersion, deserialized.protocolVersion)
        assertEquals(card.capabilities?.streaming, deserialized.capabilities?.streaming)
        assertEquals(card.skills.size, deserialized.skills.size)
        assertEquals(card.skills[0].id, deserialized.skills[0].id)
    }

    @Test
    fun `deserialize Agent Card with API key security`() {
        val cardJson =
            """
            {
                "name": "Secured Agent",
                "url": "https://secured-agent.example.com/a2a",
                "securitySchemes": {
                    "apiKey": {
                        "type": "apiKey",
                        "name": "X-API-Key",
                        "in": "header"
                    }
                },
                "security": [{"apiKey": []}]
            }
            """.trimIndent()

        val card = json.decodeFromString<AgentCard>(cardJson)
        val apiKey = card.securitySchemes["apiKey"]
        assertNotNull(apiKey)
        assertEquals("apiKey", apiKey.type)
        assertEquals("X-API-Key", apiKey.name)
        assertEquals("header", apiKey.location)
    }

    @Test
    fun `deserialize Agent Card with unknown fields gracefully`() {
        val cardJson =
            """
            {
                "name": "Future Agent",
                "url": "https://future.example.com/a2a",
                "futureField": "some value",
                "anotherUnknown": 42
            }
            """.trimIndent()

        val card = json.decodeFromString<AgentCard>(cardJson)
        assertEquals("Future Agent", card.name)
        assertEquals("https://future.example.com/a2a", card.url)
    }

    @Test
    fun `deserialize A2A v1_0 Agent Card with supportedInterfaces and preferredTransport`() {
        val cardJson =
            """
            {
                "name": "A2A Multi-Skill Test Server",
                "description": "Compliant with A2A Protocol v1.0.",
                "url": "https://a2a-test-server.example.com",
                "version": "1.0.0",
                "protocolVersion": "1.0",
                "supportedInterfaces": [
                    {
                        "url": "https://a2a-test-server.example.com/rpc",
                        "protocolBinding": "JSONRPC"
                    }
                ],
                "preferredTransport": "JSONRPC",
                "capabilities": {
                    "streaming": true,
                    "pushNotifications": true
                },
                "skills": [
                    {
                        "id": "math_calculator",
                        "name": "Math Calculator",
                        "description": "Performs basic math operations.",
                        "tags": ["math", "calculator"],
                        "examples": ["10 multiply by 4"]
                    }
                ]
            }
            """.trimIndent()

        val card = json.decodeFromString<AgentCard>(cardJson)
        assertEquals("A2A Multi-Skill Test Server", card.name)
        assertEquals("1.0", card.protocolVersion)
        assertEquals("JSONRPC", card.preferredTransport)
        assertEquals(1, card.supportedInterfaces.size)
        assertEquals("https://a2a-test-server.example.com/rpc", card.supportedInterfaces[0].url)
        assertEquals("JSONRPC", card.supportedInterfaces[0].protocolBinding)
    }
}

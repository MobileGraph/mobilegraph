package io.mobilegraph.a2a.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * A2A Agent Card — the core discovery document for an A2A agent.
 *
 * Agent Cards are typically hosted at `/.well-known/agent-card.json`
 * and describe the agent's identity, capabilities, skills, and
 * authentication requirements.
 *
 * Fully compliant with A2A Protocol v1.0 (and backward-compatible with 0.2/0.3).
 */
@Serializable
data class AgentCard(
    /** Human-readable name of the agent. */
    val name: String,
    /** Human-readable description of what this agent does. */
    val description: String? = null,
    /** The A2A JSON-RPC endpoint URL for this agent. */
    val url: String,
    /** Version of this agent. */
    val version: String? = null,
    /** A2A protocol version supported by this agent (e.g. "1.0.0", "1.0", "0.3"). */
    val protocolVersion: String? = "1.0.0",
    /** Supported interfaces (A2A v1.0). */
    val supportedInterfaces: List<AgentInterface> = emptyList(),
    /** Preferred transport protocol (e.g., "JSONRPC", "SSE", "HTTP") (A2A v1.0). */
    val preferredTransport: String? = null,
    /** Capabilities supported by this agent. */
    val capabilities: AgentCardCapabilities? = null,
    /** Skills (services/functions) this agent provides. */
    val skills: List<AgentSkill> = emptyList(),
    /** Security/authentication schemes required by this agent. */
    val securitySchemes: Map<String, SecurityScheme> = emptyMap(),
    /** Active security requirements (references to securitySchemes keys). */
    val security: List<Map<String, List<String>>> = emptyList(),
    /** Default input content modes accepted by this agent. */
    val defaultInputModes: List<String> = listOf("text/plain"),
    /** Default output content modes produced by this agent. */
    val defaultOutputModes: List<String> = listOf("text/plain"),
    /** Provider metadata. */
    val provider: AgentProvider? = null,
    /** URL to agent documentation. */
    val documentationUrl: String? = null,
    /** Whether the agent supports an authenticated extended card endpoint. */
    val supportsAuthenticatedExtendedCard: Boolean? = null,
)

/**
 * Supported interface definition for an A2A agent (A2A v1.0).
 */
@Serializable
data class AgentInterface(
    /** The interface endpoint URL. */
    val url: String,
    /** Protocol binding (e.g., "JSONRPC", "HTTP", "SSE"). */
    val protocolBinding: String? = null,
)

/**
 * Capabilities advertised by an A2A agent in its Agent Card.
 */
@Serializable
data class AgentCardCapabilities(
    /** Whether this agent supports SSE streaming via `message/stream`. */
    val streaming: Boolean? = null,
    /** Whether this agent supports push notifications. */
    val pushNotifications: Boolean? = null,
    /** Whether task status history is available. */
    val stateTransitionHistory: Boolean? = null,
)

/**
 * A skill represents a specific capability or function that an agent offers.
 *
 * Skills allow clients to understand what the agent can do before
 * sending a task/message.
 */
@Serializable
data class AgentSkill(
    /** Unique identifier for this skill. */
    val id: String,
    /** Human-readable name of the skill. */
    val name: String,
    /** Description of what this skill does. */
    val description: String? = null,
    /** Tags for categorization and search. */
    val tags: List<String> = emptyList(),
    /** Example prompts or usage descriptions. */
    val examples: List<String> = emptyList(),
    /** Input content modes accepted by this skill. */
    val inputModes: List<String> = emptyList(),
    /** Output content modes produced by this skill. */
    val outputModes: List<String> = emptyList(),
)

/**
 * Provider metadata — information about the organization
 * that created/maintains the agent.
 */
@Serializable
data class AgentProvider(
    /** Name of the organization. */
    val organization: String,
    /** URL of the organization. */
    val url: String? = null,
)

/**
 * Security scheme definition following OpenAPI-style security schemes.
 *
 * Defines how a client must authenticate with the agent.
 */
@Serializable
data class SecurityScheme(
    /** Type of security scheme: apiKey, http, oauth2, openIdConnect. */
    val type: String,
    /** Description of the security scheme. */
    val description: String? = null,
    /** Name of the header, query, or cookie parameter (for apiKey type). */
    val name: String? = null,
    /** Location of the API key: query, header, cookie (for apiKey type). */
    @SerialName("in")
    val location: String? = null,
    /** HTTP auth scheme (e.g., "bearer") (for http type). */
    val scheme: String? = null,
    /** Bearer token format hint (for http type). */
    val bearerFormat: String? = null,
    /** OAuth2 flows (for oauth2 type). */
    val flows: JsonObject? = null,
    /** OpenID Connect discovery URL (for openIdConnect type). */
    val openIdConnectUrl: String? = null,
)

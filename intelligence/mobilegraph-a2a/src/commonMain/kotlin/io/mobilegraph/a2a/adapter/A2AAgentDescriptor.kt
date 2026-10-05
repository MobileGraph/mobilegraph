package io.mobilegraph.a2a.adapter

import io.mobilegraph.a2a.models.AgentCard
import io.mobilegraph.a2a.models.AgentCardCapabilities
import io.mobilegraph.a2a.models.AgentInterface
import io.mobilegraph.a2a.models.AgentSkill
import io.mobilegraph.a2a.models.SecurityScheme

/**
 * MobileGraph's typed representation of a discovered remote A2A agent.
 *
 * This is the MobileGraph domain object that wraps a parsed [AgentCard].
 * It provides convenient accessors and capability checks so that
 * application code doesn't need to work directly with protocol types.
 *
 * Fully compliant with A2A Protocol v1.0.
 *
 * ## Usage
 * ```kotlin
 * val descriptor = A2AAgentDescriptor.from(agentCard)
 * if (descriptor.supportsStreaming) {
 *     // Use streaming
 * }
 * val skill = descriptor.findSkill("code-review")
 * ```
 */
data class A2AAgentDescriptor(
    /** Human-readable name of the remote agent. */
    val name: String,
    /** Description of the remote agent. */
    val description: String?,
    /** The A2A JSON-RPC endpoint URL. */
    val url: String,
    /** A2A protocol version supported by the remote agent. */
    val protocolVersion: String?,
    /** Version of the remote agent. */
    val agentVersion: String?,
    /** Supported interfaces (A2A v1.0). */
    val supportedInterfaces: List<AgentInterface> = emptyList(),
    /** Preferred transport protocol (A2A v1.0). */
    val preferredTransport: String? = null,
    /** Capabilities advertised by the remote agent. */
    val capabilities: AgentCardCapabilities,
    /** Skills offered by the remote agent. */
    val skills: List<AgentSkill>,
    /** Security schemes required for authentication. */
    val securitySchemes: Map<String, SecurityScheme>,
    /** Default input content modes. */
    val defaultInputModes: List<String>,
    /** Default output content modes. */
    val defaultOutputModes: List<String>,
) {
    /** Whether the remote agent supports SSE streaming. */
    val supportsStreaming: Boolean
        get() = capabilities.streaming == true

    /** Whether the remote agent supports push notifications. */
    val supportsPushNotifications: Boolean
        get() = capabilities.pushNotifications == true

    /** Whether the remote agent supports task state transition history. */
    val supportsStateHistory: Boolean
        get() = capabilities.stateTransitionHistory == true

    /** Whether the remote agent requires authentication. */
    val requiresAuthentication: Boolean
        get() = securitySchemes.isNotEmpty()

    /**
     * Finds a skill by its ID.
     *
     * @param skillId The skill ID to search for.
     * @return The matching [AgentSkill], or null.
     */
    fun findSkill(skillId: String): AgentSkill? = skills.firstOrNull { it.id == skillId }

    /**
     * Finds skills matching any of the given tags.
     *
     * @param tags Tags to match against.
     * @return List of matching skills.
     */
    fun findSkillsByTag(vararg tags: String): List<AgentSkill> =
        skills.filter { skill ->
            skill.tags.any { it in tags }
        }

    companion object {
        /**
         * Creates an [A2AAgentDescriptor] from an [AgentCard].
         */
        fun from(card: AgentCard): A2AAgentDescriptor =
            A2AAgentDescriptor(
                name = card.name,
                description = card.description,
                url = card.url,
                protocolVersion = card.protocolVersion ?: "1.0.0",
                agentVersion = card.version,
                supportedInterfaces = card.supportedInterfaces,
                preferredTransport = card.preferredTransport,
                capabilities = card.capabilities ?: AgentCardCapabilities(),
                skills = card.skills,
                securitySchemes = card.securitySchemes,
                defaultInputModes = card.defaultInputModes,
                defaultOutputModes = card.defaultOutputModes,
            )
    }
}

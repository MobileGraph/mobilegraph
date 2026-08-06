package io.mobilegraph.skills

import io.mobilegraph.core.tools.Tool

/**
 * Represents a bundled capability in the MobileGraph SDK.
 * A skill combines expertise (instructions) with capabilities (tools).
 */
data class Skill(
    /**
     * The unique name of the skill.
     */
    val name: String,
    /**
     * A brief description of what the skill does.
     */
    val description: String?,
    /**
     * Detailed instructions or expertise for the LLM to follow when using this skill.
     */
    val instructions: String,
    /**
     * The actual tool implementations provided for this skill.
     */
    val tools: List<Tool<*, *>>,
    /**
     * A list of tool names that were mentioned in the source (e.g., Markdown) but might or might not have implementations.
     */
    val declaredTools: List<String> = emptyList(),
)

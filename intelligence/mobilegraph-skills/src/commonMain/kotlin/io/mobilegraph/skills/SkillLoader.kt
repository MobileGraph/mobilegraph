package io.mobilegraph.skills

import io.mobilegraph.core.tools.Tool
import io.mobilegraph.skills.parser.MarkdownSkillParser

/**
 * Entry point for loading skills from external sources.
 */
object SkillLoader {
    private val parser = MarkdownSkillParser()

    /**
     * Loads a skill from a Markdown string and binds it to provided tool implementations.
     *
     * @param content The raw Markdown string containing skill instructions and tool definitions.
     * @param tools A list of Kotlin [Tool] implementations required by this skill.
     * @param nameFallback An optional name to use if the Markdown doesn't contain a header.
     * @return A [SkillLoadResult] containing the parsed skill and any validation warnings.
     */
    fun fromMarkdown(
        content: String,
        tools: List<Tool<*, *>>,
        nameFallback: String? = null,
    ): SkillLoadResult {
        val parsed = parser.parse(content)
        val warnings = mutableListOf<String>()

        // 1. Resolve Name (Markdown H1 > Kotlin Fallback > Default)
        val resolvedName = parsed.name ?: nameFallback ?: "skill_${content.hashCode().coerceAtLeast(0)}"
        if (parsed.name == null && nameFallback == null) {
            warnings.add("No name found in Markdown (H1) or Kotlin fallback. Generated name: $resolvedName")
        }

        // 2. Validate Tools (Two-Way Check)
        val providedToolNames = tools.map { it.metadata.name }.toSet()
        val declaredToolNames = parsed.declaredTools.toSet()

        // Markdown -> Kotlin check
        declaredToolNames.forEach { name ->
            if (!providedToolNames.contains(name)) {
                warnings.add("Skill '$resolvedName' mentions tool '$name' in Markdown, but no implementation was provided.")
            }
        }

        // Kotlin -> Markdown check
        providedToolNames.forEach { name ->
            if (!declaredToolNames.contains(name)) {
                warnings.add(
                    "Implementation for tool '$name' was provided for skill '$resolvedName', but it is not mentioned in the Markdown instructions.",
                )
            }
        }

        val skill =
            Skill(
                name = resolvedName,
                description = parsed.description,
                instructions = parsed.instructions,
                tools = tools,
                declaredTools = parsed.declaredTools,
            )

        return SkillLoadResult(skill, warnings)
    }
}

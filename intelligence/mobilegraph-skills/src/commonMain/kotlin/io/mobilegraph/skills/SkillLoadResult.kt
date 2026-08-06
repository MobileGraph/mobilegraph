package io.mobilegraph.skills

/**
 * The result of loading a skill from an external source like Markdown.
 */
data class SkillLoadResult(
    /**
     * The successfully parsed skill.
     */
    val skill: Skill,
    /**
     * A list of warnings discovered during loading (e.g., missing tool implementations).
     */
    val warnings: List<String> = emptyList(),
)

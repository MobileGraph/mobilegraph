package io.mobilegraph.skills

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkillLoaderTest {
    @Serializable
    class MockInput

    private class MockTool(
        override val metadata: ToolMetadata,
    ) : Tool<MockInput, String> {
        override val inputSerializer: KSerializer<MockInput> = MockInput.serializer()

        override suspend fun invoke(
            input: MockInput,
            context: ExecutionContext,
        ): String = "ok"
    }

    @Test
    fun testNameResolutionPriority() {
        val content = "# Markdown Name\n## Instructions\nDo stuff."

        // 1. Markdown H1 should win over Kotlin fallback
        val result1 = SkillLoader.fromMarkdown(content, emptyList(), nameFallback = "Kotlin Name")
        assertEquals("Markdown Name", result1.skill.name)

        // 2. Kotlin fallback should win if no H1
        val result2 = SkillLoader.fromMarkdown("## Instructions\nDo stuff.", emptyList(), nameFallback = "Kotlin Name")
        assertEquals("Kotlin Name", result2.skill.name)

        // 3. Default name if both missing
        val result3 = SkillLoader.fromMarkdown("## Instructions\nDo stuff.", emptyList())
        assertTrue(result3.skill.name.startsWith("skill_"))
        assertTrue(result3.warnings.any { it.contains("No name found") })
    }

    @Test
    fun testToolValidationWarnings() {
        val content =
            """
            # Test
            ## Tools
            - `tool_a`
            - `tool_b`
            """.trimIndent()

        val toolA = MockTool(ToolMetadata("tool_a", "A"))
        val toolC = MockTool(ToolMetadata("tool_c", "C"))

        // tool_a is matched
        // tool_b is missing in Kotlin
        // tool_c is extra in Kotlin (not in Markdown)
        val result = SkillLoader.fromMarkdown(content, listOf(toolA, toolC))

        assertEquals(2, result.warnings.size)
        assertTrue(result.warnings.any { it.contains("tool_b") && it.contains("no implementation") })
        assertTrue(result.warnings.any { it.contains("tool_c") && it.contains("not mentioned") })

        assertEquals(2, result.skill.tools.size)
        assertEquals(2, result.skill.declaredTools.size)
    }

    @Test
    fun testPerfectMatchNoWarnings() {
        val content =
            """
            # Test
            ## Tools
            - `tool_a`
            """.trimIndent()

        val toolA = MockTool(ToolMetadata("tool_a", "A"))
        val result = SkillLoader.fromMarkdown(content, listOf(toolA))

        assertTrue(result.warnings.isEmpty())
        assertEquals(
            "tool_a",
            result.skill.tools[0]
                .metadata.name,
        )
    }
}

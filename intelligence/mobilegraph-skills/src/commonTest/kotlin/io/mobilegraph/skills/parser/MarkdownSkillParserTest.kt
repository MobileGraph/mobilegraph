package io.mobilegraph.skills.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownSkillParserTest {
    private val parser = MarkdownSkillParser()

    @Test
    fun testParseFullMarkdown() {
        val content =
            """
            # Test Skill
            This is a test description.
            It spans multiple lines.
            
            ## Instructions
            - Rule 1
            - Rule 2
            
            ## Tools
            - `tool_one`: Description one
            - `tool_two`
            * tool_three
            """.trimIndent()

        val parsed = parser.parse(content)

        assertEquals("Test Skill", parsed.name)
        assertEquals("This is a test description.\nIt spans multiple lines.", parsed.description)
        assertTrue(parsed.instructions.contains("Rule 1"))
        assertTrue(parsed.instructions.contains("Rule 2"))
        assertEquals(3, parsed.declaredTools.size)
        assertTrue(parsed.declaredTools.contains("tool_one"))
        assertTrue(parsed.declaredTools.contains("tool_two"))
        assertTrue(parsed.declaredTools.contains("tool_three"))
    }

    @Test
    fun testParseNoH1() {
        val content =
            """
            ## Instructions
            Follow these rules.
            """.trimIndent()

        val parsed = parser.parse(content)

        assertEquals(null, parsed.name)
        assertEquals(null, parsed.description)
        assertEquals("Follow these rules.", parsed.instructions)
    }

    @Test
    fun testAlternativeHeaders() {
        val content =
            """
            # Header
            
            ## Rules
            Be nice.
            
            ## Capabilities
            - `action_one`
            """.trimIndent()

        val parsed = parser.parse(content)

        assertEquals("Header", parsed.name)
        assertEquals("Be nice.", parsed.instructions)
        assertEquals(listOf("action_one"), parsed.declaredTools)
    }

    @Test
    fun testEmptyContent() {
        val parsed = parser.parse("")
        assertEquals(null, parsed.name)
        assertEquals(null, parsed.description)
        assertEquals("", parsed.instructions)
        assertTrue(parsed.declaredTools.isEmpty())
    }

    @Test
    fun testToolNameExtractionVariants() {
        val content =
            """
            ## Tools
            - `backtick_tool`
            * plain_list_tool
            - dash_tool
            * `mixed` tool
            """.trimIndent()

        val parsed = parser.parse(content)
        assertEquals(listOf("backtick_tool", "plain_list_tool", "dash_tool", "mixed"), parsed.declaredTools)
    }
}

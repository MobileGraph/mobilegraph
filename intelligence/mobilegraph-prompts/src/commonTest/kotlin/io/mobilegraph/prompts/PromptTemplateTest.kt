package io.mobilegraph.prompts

import io.mobilegraph.prompts.exceptions.MissingVariableException
import io.mobilegraph.prompts.templates.PromptTemplate
import io.mobilegraph.prompts.variables.Variables
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PromptTemplateTest {
    @Test
    fun testSimpleSubstitution() {
        val template = PromptTemplate("Hello, {name}!")
        val variables = Variables.of("name" to "World")
        assertEquals("Hello, World!", template.render(variables))
    }

    @Test
    fun testMultipleSubstitutions() {
        val template = PromptTemplate("{greeting}, {name}!")
        val variables = Variables.of("greeting" to "Hi", "name" to "MobileGraph")
        assertEquals("Hi, MobileGraph!", template.render(variables))
    }

    @Test
    fun testMissingVariable() {
        val template = PromptTemplate("Hello, {name}!")
        assertFailsWith<MissingVariableException> {
            template.render(Variables.Empty)
        }
    }

    @Test
    fun testComposition() {
        val t1 = PromptTemplate("Part 1. ")
        val t2 = PromptTemplate("Part 2: {val}")
        val combined = t1.append(t2)

        assertEquals("Part 1. Part 2: 42", combined.render(Variables.Empty.with("val", 42)))
    }

    @Test
    fun testImmutability() {
        val template = PromptTemplate("Base")
        val extended = template.append(" Extended")

        assertEquals("Base", template.template)
        assertEquals("Base Extended", extended.template)
    }

    @Test
    fun testOptionalVariable() {
        val template = PromptTemplate("Hello{name?}!")
        assertEquals("Hello!", template.render(Variables.Empty))
        assertEquals("Hello World!", template.render(Variables.of("name" to " World")))
    }

    @Test
    fun testEscaping() {
        val template = PromptTemplate("JSON: {{ \"key\": \"{val}\" }}")
        assertEquals("JSON: { \"key\": \"42\" }", template.render(Variables.Empty.with("val", 42)))
    }
}

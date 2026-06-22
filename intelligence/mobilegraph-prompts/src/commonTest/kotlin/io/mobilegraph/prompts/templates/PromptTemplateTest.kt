/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.prompts.templates

import io.mobilegraph.prompts.variables.Variables
import kotlin.test.Test
import kotlin.test.assertEquals

class PromptTemplateTest {
    @Test
    fun testBasicSubstitution() {
        val template = PromptTemplate("Hello {name}!")
        val variables = Variables.of("name" to "Alice")

        assertEquals("Hello Alice!", template.render(variables))
    }

    @Test
    fun testMultipleSubstitutions() {
        val template = PromptTemplate("{greeting}, {name}!")
        val variables = Variables.of("greeting" to "Hi", "name" to "Bob")

        assertEquals("Hi, Bob!", template.render(variables))
    }

    @Test
    fun testOptionalVariable() {
        val template = PromptTemplate("Hello{name?}")

        // Present
        assertEquals("HelloAlice", template.render(Variables.of("name" to "Alice")))

        // Absent
        assertEquals("Hello", template.render(Variables.Empty))
    }

    @Test
    fun testLiteralBraces() {
        val template = PromptTemplate("JSON: {{ \"key\": \"{value}\" }}")
        val variables = Variables.of("value" to "val1")

        assertEquals("JSON: { \"key\": \"val1\" }", template.render(variables))
    }
}

/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.parsers

import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ModelOutput
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StructuredOutputParserTest {
    @Serializable
    data class TestData(
        val name: String,
        val age: Int,
    )

    @Test
    fun testSuccessfulParse() {
        val parser = structuredOutputParser<TestData>()
        val output = ModelOutput.ChatOutput(AssistantMessage("{\"name\": \"Alice\", \"age\": 30}"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals("Alice", result.value.name)
        assertEquals(30, result.value.age)
    }

    @Test
    fun testParseWithProse() {
        val parser = structuredOutputParser<TestData>()
        val output = ModelOutput.ChatOutput(AssistantMessage("Here is the data: {\"name\": \"Bob\", \"age\": 25} Hope this helps!"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals("Bob", result.value.name)
    }

    @Test
    fun testMalformedJson() {
        val parser = structuredOutputParser<TestData>()
        val output = ModelOutput.ChatOutput(AssistantMessage("{\"name\": \"Alice\", age: 30}")) // Missing quotes

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Failure)
    }

    @Test
    fun testNoJsonObject() {
        val parser = structuredOutputParser<TestData>()
        val output = ModelOutput.ChatOutput(AssistantMessage("No data here"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Failure)
        assertEquals("No JSON object found in output", result.error.message)
    }

    @Test
    fun testSchemaGeneration() {
        val parser = structuredOutputParser<TestData>()
        val instructions = parser.formatInstructions()

        assertTrue(instructions.contains("\"name\": \"string\""))
        assertTrue(instructions.contains("\"age\": \"number\""))
        assertTrue(instructions.contains("Return a JSON object matching this schema"))
    }

    @Serializable
    data class NestedData(
        val items: List<String>,
        val meta: TestData,
    )

    @Test
    fun testNestedSchemaAndParse() {
        val parser = structuredOutputParser<NestedData>()
        val instructions = parser.formatInstructions()

        assertTrue(instructions.contains("\"items\": [\"string\"]"))
        assertTrue(instructions.contains("\"meta\": {\"name\": \"string\", \"age\": \"number\"}"))

        val json =
            """
            {
                "items": ["a", "b"],
                "meta": {"name": "Charlie", "age": 40}
            }
            """.trimIndent()
        val output = ModelOutput.ChatOutput(AssistantMessage(json))
        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals(2, result.value.items.size)
        assertEquals("Charlie", result.value.meta.name)
    }
}

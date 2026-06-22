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
}

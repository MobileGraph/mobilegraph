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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListParserTest {
    @Test
    fun testSimpleList() {
        val parser = listParser()
        val output = ModelOutput.ChatOutput(AssistantMessage("Apple\nBanana\nOrange"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals(listOf("Apple", "Banana", "Orange"), result.value)
    }

    @Test
    fun testDashedList() {
        val parser = listParser()
        val output = ModelOutput.ChatOutput(AssistantMessage("- Item 1\n- Item 2\n- Item 3"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals(listOf("Item 1", "Item 2", "Item 3"), result.value)
    }

    @Test
    fun testEmptyList() {
        val parser = listParser()
        val output = ModelOutput.ChatOutput(AssistantMessage("\n\n"))

        val result = parser.parse(output)

        assertTrue(result is ParseResult.Success)
        assertEquals(emptyList<String>(), result.value)
    }
}

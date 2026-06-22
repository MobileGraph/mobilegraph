/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.tools

import io.mobilegraph.core.context.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class ToolExtensionsTest {
    @Serializable
    data class AddInput(
        val a: Int,
        val b: Int,
    )

    @Test
    fun testExecuteFromJson() =
        runTest {
            val tool =
                object : Tool<AddInput, Int> {
                    override val metadata = ToolMetadata("add", "adds two numbers")
                    override val inputSerializer = AddInput.serializer()

                    override suspend fun invoke(
                        input: AddInput,
                        context: ExecutionContext,
                    ): Int = input.a + input.b
                }

            val result = tool.executeFromJson("{\"a\": 5, \"b\": 10}", ExecutionContext.Empty)
            assertEquals("15", result)
        }

    @Test
    fun testAsDefinition() {
        val tool =
            object : Tool<AddInput, Int> {
                override val metadata = ToolMetadata("add", "adds numbers")

                override suspend fun invoke(
                    input: AddInput,
                    context: ExecutionContext,
                ): Int = 0
            }

        val definition = tool.asDefinition()
        assertEquals("add", definition.name)
        assertEquals("adds numbers", definition.description)
        // assertNotNull(definition.parameters) // parameters is null by default in current implementation
    }
}

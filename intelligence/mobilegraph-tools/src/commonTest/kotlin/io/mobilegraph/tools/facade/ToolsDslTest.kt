/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.tools.facade

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.core.tools.ToolSelector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ToolsDslTest {
    @Test
    fun testWithToolsDsl() {
        val tool =
            object : Tool<String, String> {
                override val metadata = ToolMetadata("t1", "desc")

                override suspend fun invoke(
                    input: String,
                    context: ExecutionContext,
                ) = ""
            }

        val env =
            MobileGraphEnvironment
                .Builder()
                .withTools {
                    register(tool)
                }.build()

        val registry = env.getComponent(ToolRegistry::class)
        assertNotNull(registry)
        assertEquals(tool, registry.get("t1"))
    }

    @Test
    fun testWithToolSelector() {
        val mockSelector =
            object : ToolSelector {
                override suspend fun selectTools(
                    query: String,
                    registry: ToolRegistry,
                    context: ExecutionContext,
                ) = emptyList<Tool<*, *>>()
            }

        val env = MobileGraphEnvironment.Builder().withToolSelector(mockSelector).build()

        val selector = env.getComponent(ToolSelector::class)
        assertEquals(mockSelector, selector)
    }
}

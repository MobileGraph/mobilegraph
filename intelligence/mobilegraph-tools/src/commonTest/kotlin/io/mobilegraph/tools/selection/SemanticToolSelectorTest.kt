/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.tools.selection

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.models.EmbeddingModel
import io.mobilegraph.tools.registry.DefaultToolRegistry
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticToolSelectorTest {
    private class MockEmbeddingModel(
        val embeddings: Map<String, List<Float>>,
    ) : EmbeddingModel {
        override val name: String = "mock"

        override fun supports(capability: Capability): Boolean = false

        override suspend fun embed(
            text: String,
            context: ExecutionContext,
        ): List<Float> =
            embeddings.entries
                .find {
                    text.contains(it.key)
                }?.value ?: listOf(0f, 0f)

        override suspend fun embedBatch(
            texts: List<String>,
            context: ExecutionContext,
        ): List<List<Float>> = texts.map { embed(it, context) }
    }

    @Test
    fun testToolSelection() =
        runTest {
            val weatherTool =
                object : Tool<String, String> {
                    override val metadata = ToolMetadata("weather", "Gets weather info")

                    override suspend fun invoke(
                        input: String,
                        context: ExecutionContext,
                    ): String = ""
                }

            val calcTool =
                object : Tool<String, String> {
                    override val metadata = ToolMetadata("calc", "Performs math")

                    override suspend fun invoke(
                        input: String,
                        context: ExecutionContext,
                    ): String = ""
                }

            val registry = DefaultToolRegistry()
            registry.register(weatherTool)
            registry.register(calcTool)

            val embeddings =
                mapOf(
                    "weather" to listOf(1f, 0f),
                    "calc" to listOf(0f, 1f),
                    "rain" to listOf(1f, 0f),
                    "sum" to listOf(0f, 1f),
                )

            val selector =
                SemanticToolSelector(
                    embeddingModel = MockEmbeddingModel(embeddings),
                    threshold = 0.5f,
                )

            val selected = selector.selectTools("will it rain?", registry, ExecutionContext.Empty)

            assertEquals(1, selected.size)
            assertEquals("weather", selected[0].metadata.name)
        }

    @Test
    fun testNoToolSelectedUnderThreshold() =
        runTest {
            val tool =
                object : Tool<String, String> {
                    override val metadata = ToolMetadata("tool", "desc")

                    override suspend fun invoke(
                        input: String,
                        context: ExecutionContext,
                    ): String = ""
                }
            val registry = DefaultToolRegistry()
            registry.register(tool)

            val selector =
                SemanticToolSelector(
                    embeddingModel = MockEmbeddingModel(mapOf("tool" to listOf(1f, 0f))),
                    threshold = 0.9f,
                )

            val selected = selector.selectTools("unrelated", registry, ExecutionContext.Empty)
            assertEquals(0, selected.size)
        }
}

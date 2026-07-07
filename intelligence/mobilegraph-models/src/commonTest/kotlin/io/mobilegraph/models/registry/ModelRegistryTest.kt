/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.registry

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelRegistryTest {
    private class MockChatModel(
        override val name: String,
        val hasVision: Boolean = false,
    ) : ChatModel {
        override fun supports(capability: Capability): Boolean = hasVision && capability == Capability.Vision

        override suspend fun invoke(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: io.mobilegraph.core.context.ExecutionContext,
        ): ModelOutput = ModelOutput.ErrorOutput(Exception())

        override fun stream(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: io.mobilegraph.core.context.ExecutionContext,
        ): Flow<ChatChunk> = emptyFlow()

        override fun readModelConfig(): ModelConfig? = null
    }

    @Test
    fun testChatModelRegistration() {
        val registry = DefaultModelRegistry()
        val model1 = MockChatModel("m1")
        val model2 = MockChatModel("m2")

        registry.registerChat("m1", model1, isDefault = true)
        registry.registerChat("m2", model2)

        assertEquals(model1, registry.chat("m1"))
        assertEquals(model2, registry.chat("m2"))
        assertEquals(model1, registry.chat()) // Default
    }

    @Test
    fun testCapabilitySearch() {
        val registry = DefaultModelRegistry()
        val visionModel = MockChatModel("vision", hasVision = true)

        registry.registerChat("vision", visionModel)

        assertEquals(visionModel, registry.chatFor(Capability.Vision))
        assertNull(registry.chatFor(Capability.Streaming))
    }
}

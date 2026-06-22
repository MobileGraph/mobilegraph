package io.mobilegraph.models.facade

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.ModelRegistry
import io.mobilegraph.models.middleware.ChatModelInput
import io.mobilegraph.models.middleware.ChatModelMiddleware
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelDslTest {
    private class MockChatModel(
        override val name: String = "mock",
    ) : ChatModel {
        var lastConfig: ModelConfig? = null

        override suspend fun invoke(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ): ModelOutput {
            lastConfig = config
            return ModelOutput.ChatOutput(AssistantMessage("resp"))
        }

        override fun stream(
            prompt: ChatPromptValue,
            config: ModelConfig?,
            context: ExecutionContext,
        ) = emptyFlow<ChatChunk>()

        override fun supports(capability: Capability): Boolean = true
    }

    @Test
    fun testChatModelDsl() {
        val mockModel = MockChatModel("gpt-4")
        val env =
            MobileGraphEnvironment
                .Builder()
                .withModels {
                    chat(mockModel) {
                        isDefault = true
                        defaultConfig {
                            temperature = 0.7f
                            maxTokens = 100
                        }
                    }
                }.build()

        val registry = env.getComponent(ModelRegistry::class)
        assertNotNull(registry)

        val registeredModel = registry.chat("gpt-4")
        assertNotNull(registeredModel)
        assertEquals("gpt-4", registeredModel.name)
    }

    @Test
    fun testMiddlewareDsl() {
        var intercepted = false
        val middleware =
            object : ChatModelMiddleware {
                override suspend fun intercept(
                    input: ChatModelInput,
                    context: ExecutionContext,
                    next: suspend (ChatModelInput, ExecutionContext) -> ModelOutput,
                ): ModelOutput {
                    intercepted = true
                    return next(input, context)
                }
            }

        val mockModel = MockChatModel("test")
        val env =
            MobileGraphEnvironment
                .Builder()
                .withModels {
                    chat(mockModel) {
                        middleware {
                            +middleware
                        }
                    }
                }.build()

        val registeredModel = env.getComponent(ModelRegistry::class)?.chat("test")
        assertNotNull(registeredModel)

        // Invoke to trigger middleware
        kotlinx.coroutines.test.runTest {
            registeredModel.invoke(ChatPromptValue(emptyList()))
            assertTrue(intercepted)
        }
    }

    @Test
    fun testConfigInheritance() =
        kotlinx.coroutines.test.runTest {
            val mockModel = MockChatModel("test")
            val env =
                MobileGraphEnvironment
                    .Builder()
                    .withModels {
                        chat(mockModel) {
                            defaultConfig {
                                temperature = 0.5f
                                maxTokens = 50
                            }
                        }
                    }.build()

            val registeredModel = env.getComponent(ModelRegistry::class)?.chat("test")
            assertNotNull(registeredModel)

            // Test default config
            registeredModel.invoke(ChatPromptValue(emptyList()))
            assertEquals(0.5f, mockModel.lastConfig?.temperature)
            assertEquals(50, mockModel.lastConfig?.maxTokens)

            // Test override
            registeredModel.invoke(ChatPromptValue(emptyList()), ModelConfig(temperature = 0.9f))
            assertEquals(0.9f, mockModel.lastConfig?.temperature)
            assertEquals(50, mockModel.lastConfig?.maxTokens) // maxTokens still from default
        }
}

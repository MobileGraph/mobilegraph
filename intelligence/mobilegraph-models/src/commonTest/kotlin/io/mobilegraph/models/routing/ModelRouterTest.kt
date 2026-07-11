package io.mobilegraph.models.routing

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.ModelRegistry
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelRouterTest {
    private class MockModel(
        override val name: String,
    ) : ChatModel {
        override fun supports(capability: io.mobilegraph.core.capability.Capability): Boolean = true

        override suspend fun invoke(
            p: ChatPromptValue,
            c: ModelConfig?,
            ctx: ExecutionContext,
        ) = ModelOutput.ChatOutput(AssistantMessage("Response from $name"))

        override fun stream(
            p: ChatPromptValue,
            c: ModelConfig?,
            ctx: ExecutionContext,
        ): Flow<ChatChunk> = emptyFlow()

        override fun readModelConfig(): ModelConfig? = null
    }

    private class SimpleRegistry : ModelRegistry {
        private val models = mutableMapOf<String, ChatModel>()

        fun register(model: ChatModel) {
            models[model.name] = model
        }

        override fun chat(name: String) = models[name]

        override fun chat() = models.values.firstOrNull()

        override fun chatFor(capability: io.mobilegraph.core.capability.Capability): ChatModel? = null

        override fun embedding(name: String) = null

        override fun embedding() = null
    }

    @Test
    fun testRoutingLogic() =
        runTest {
            val registry = SimpleRegistry()
            registry.register(MockModel("m1"))
            registry.register(MockModel("m2"))
            registry.register(MockModel("default"))

            val router =
                PolicyBasedRouter(
                    name = "router",
                    registry = registry,
                    policies =
                        listOf(
                            object : RoutingPolicy {
                                override suspend fun selectModel(input: RouterInput): String? = if (input.promptLength > 10) "m1" else null
                            },
                            object : RoutingPolicy {
                                override suspend fun selectModel(input: RouterInput): String? = if (input.hasImages) "m2" else null
                            },
                        ),
                    defaultModelName = "default",
                )

            // Test Policy 1: Length
            val r1 = router.invoke(ChatPromptValue(listOf(UserMessage("this is a long prompt"))))
            assertEquals("Response from m1", (r1 as ModelOutput.ChatOutput).message.content)

            // Test Policy 2: Images
            val r2 = router.invoke(ChatPromptValue(listOf(UserMessage(listOf(ContentPart.Image("url"))))))
            assertEquals("Response from m2", (r2 as ModelOutput.ChatOutput).message.content)

            // Test Default
            val r3 = router.invoke(ChatPromptValue(listOf(UserMessage("short"))))
            assertEquals("Response from default", (r3 as ModelOutput.ChatOutput).message.content)
        }
}

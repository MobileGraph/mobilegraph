package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.flow.Flow

/**
 * A ChatModel wrapper that applies a list of middlewares to every invocation.
 */
internal class MiddlewareChatModel(
    private val delegate: ChatModel,
    private val middlewares: List<ChatModelMiddleware>,
) : ChatModel by delegate {
    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput = execute(0, ChatModelInput(prompt, config), context)

    private suspend fun execute(
        index: Int,
        input: ChatModelInput,
        context: ExecutionContext,
    ): ModelOutput {
        if (index >= middlewares.size) {
            return delegate.invoke(input.prompt, input.config, context)
        }

        return middlewares[index].intercept(input, context) { nextInput, nextContext ->
            execute(index + 1, nextInput, nextContext)
        }
    }

    // Streaming middleware is more complex (requires Flow transformation),
    // for now we delegate directly.
    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): Flow<ChatChunk> = delegate.stream(prompt, config, context)

    override fun readModelConfig(): ModelConfig? = delegate.readModelConfig()
}

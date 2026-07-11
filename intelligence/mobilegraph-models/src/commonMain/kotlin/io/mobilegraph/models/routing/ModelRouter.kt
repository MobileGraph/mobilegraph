package io.mobilegraph.models.routing

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.ModelRegistry
import kotlinx.coroutines.flow.Flow

/**
 * A [ChatModel] that delegates to other models based on a set of rules or policies.
 */
interface ModelRouter : ChatModel {
    /**
     * Determines which model should be used for the given input.
     */
    suspend fun route(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): String
}

/**
 * Data class representing the input for routing decisions.
 */
data class RouterInput(
    val prompt: ChatPromptValue,
    val config: ModelConfig?,
    val context: ExecutionContext,
)

/**
 * Interface for a routing policy.
 */
interface RoutingPolicy {
    /**
     * Evaluates the policy and returns the name of the model to use, or null if it doesn't apply.
     */
    suspend fun selectModel(input: RouterInput): String?
}

/**
 * Standard implementation of a [ModelRouter] that evaluates policies in order.
 */
class PolicyBasedRouter(
    override val name: String,
    private val registry: ModelRegistry,
    private val policies: List<RoutingPolicy>,
    private val defaultModelName: String,
) : ModelRouter {
    override fun supports(capability: Capability): Boolean = true // Router itself is transparent

    override suspend fun route(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): String {
        val input = RouterInput(prompt, config, context)
        for (policy in policies) {
            val modelName = policy.selectModel(input)
            if (modelName != null) return modelName
        }
        return defaultModelName
    }

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput {
        val targetName = route(prompt, config, context)
        val model =
            registry.chat(targetName) ?: return ModelOutput.ErrorOutput(
                IllegalStateException("Routed model '$targetName' not found in registry"),
            )
        return model.invoke(prompt, config, context)
    }

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): Flow<ChatChunk> {
        // For streaming, we determine the route based on the initial request
        // Since route() is suspend, we might need a better way for pure flow.
        // For now, we block on finding the route since it should be local logic.

        // This is a simplification. Real impl would use a flow builder.
        throw UnsupportedOperationException("Streaming not yet supported via Router")
    }

    override fun readModelConfig(): ModelConfig? = null
}

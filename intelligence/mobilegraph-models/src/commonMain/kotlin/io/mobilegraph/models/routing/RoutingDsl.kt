package io.mobilegraph.models.routing

import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.ModelRegistry

/**
 * DSL Builder for creating a [ModelRouter].
 */
class RouterBuilder(
    val name: String,
    private val registry: ModelRegistry,
) {
    private val policies = mutableListOf<RoutingPolicy>()
    private var defaultModel: String? = null

    /**
     * Adds a simple rule-based policy.
     */
    fun policy(block: PolicyBuilder.() -> Unit) {
        val builder = PolicyBuilder()
        builder.block()
        policies.add(builder.build())
    }

    /**
     * Sets the fallback model if no policies match.
     */
    fun default(modelName: String) {
        defaultModel = modelName
    }

    fun build(): ModelRouter =
        PolicyBasedRouter(
            name = name,
            registry = registry,
            policies = policies,
            defaultModelName = defaultModel ?: throw IllegalStateException("Default model must be specified for router '$name'"),
        )
}

class PolicyBuilder {
    private var condition: (suspend (RouterInput) -> Boolean)? = null
    private var targetModel: String? = null

    /**
     * Defines the condition for this policy.
     */
    fun condition(block: suspend (RouterInput) -> Boolean) {
        condition = block
    }

    /**
     * Sets the model to use if the condition is met.
     */
    fun use(modelName: String) {
        targetModel = modelName
    }

    fun build() =
        object : RoutingPolicy {
            override suspend fun selectModel(input: RouterInput): String? = if (condition?.invoke(input) == true) targetModel else null
        }
}

/**
 * Convenience extensions for common routing conditions.
 */
val RouterInput.promptLength: Int
    get() = prompt.messages.sumOf { it.content.length }

val RouterInput.hasImages: Boolean
    get() =
        prompt.messages.any { msg ->
            msg.parts.any { it is ContentPart.Image }
        }

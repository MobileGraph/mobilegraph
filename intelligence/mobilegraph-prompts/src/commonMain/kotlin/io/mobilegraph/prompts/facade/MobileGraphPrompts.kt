package io.mobilegraph.prompts.facade

import io.mobilegraph.core.configuration.PromptsConfiguration
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.prompts.registry.DefaultPromptRegistry
import io.mobilegraph.prompts.registry.PromptRegistry
import io.mobilegraph.prompts.templates.PromptTemplate

/**
 * Provides access to prompt-related functionality through the MobileGraph facade.
 */
val MobileGraph.prompts: MobileGraphPrompts
    get() = MobileGraphPrompts(this)

/**
 * Provides access to prompt-related functionality through the MobileGraph companion (global instance).
 */
val MobileGraph.Companion.prompts: MobileGraphPrompts
    get() = MobileGraph.instance.prompts

/**
 * Entry point for prompt operations.
 */
class MobileGraphPrompts(
    private val mobileGraph: MobileGraph,
) {
    /**
     * Accesses the prompt registry.
     */
    fun registry(): PromptRegistry =
        mobileGraph.environment.getComponent(PromptRegistry::class)
            ?: throw IllegalStateException("PromptRegistry not found in environment")
}

/**
 * DSL for registering prompts.
 */
class PromptsBuilder {
    private val registry = DefaultPromptRegistry()

    fun register(
        name: String,
        template: String,
    ) {
        registry.register(name, PromptTemplate(template))
    }

    fun register(
        name: String,
        template: PromptTemplate,
    ) {
        registry.register(name, template)
    }

    internal fun build(): PromptRegistry = registry
}

fun PromptsConfiguration.register(
    name: String,
    template: String,
) {
    // This will be bridged via the environment builder extension
}

/**
 * Optional config. Register a prompt template by name. Which can be retrieved and use later. Can act as a central registry for prompt templates
 */
fun MobileGraphEnvironment.Builder.withPrompts(block: PromptsBuilder.() -> Unit) =
    apply {
        val builder = PromptsBuilder()
        builder.block()
        component(PromptRegistry::class, builder.build())
    }

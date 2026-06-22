package io.mobilegraph.prompts.registry

import io.mobilegraph.prompts.templates.PromptTemplate

/**
 * Registry for managing and retrieving prompt templates.
 */
interface PromptRegistry {
    /**
     * Registers a template with a unique name.
     */
    fun register(
        name: String,
        template: PromptTemplate,
    )

    /**
     * Retrieves a template by name.
     */
    fun get(name: String): PromptTemplate?
}

/**
 * Default implementation of [PromptRegistry].
 */
internal class DefaultPromptRegistry : PromptRegistry {
    private val templates = mutableMapOf<String, PromptTemplate>()

    override fun register(
        name: String,
        template: PromptTemplate,
    ) {
        templates[name] = template
    }

    override fun get(name: String): PromptTemplate? = templates[name]
}

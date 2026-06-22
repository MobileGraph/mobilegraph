package io.mobilegraph.prompts

/**
 * Interface for prompt templates.
 */
interface PromptTemplate {
    fun render(variables: Map<String, Any>): String
}

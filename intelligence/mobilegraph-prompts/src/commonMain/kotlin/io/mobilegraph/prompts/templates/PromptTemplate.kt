package io.mobilegraph.prompts.templates

import io.mobilegraph.prompts.exceptions.MissingVariableException
import io.mobilegraph.prompts.variables.Variables

/**
 * A reusable prompt template with variable substitution.
 * Supports {variable} for required variables and {variable?} for optional variables.
 * Use {{ and }} for literal braces.
 */
class PromptTemplate(
    val template: String,
) {
    private data class VariableInfo(
        val name: String,
        val isOptional: Boolean,
    )

    private val variablesInfo: List<VariableInfo> by lazy {
        variableRegex
            .findAll(template)
            .map { match ->
                val name = match.groupValues[1]
                val isOptional = match.groupValues[2] == "?"
                VariableInfo(name, isOptional)
            }.toList()
    }

    /**
     * Renders the template by substituting variables.
     * Throws MissingVariableException if a required variable is not provided.
     */
    fun render(variables: Variables): String {
        // Simple escape handling for literal braces
        var result =
            template
                .replace("{{", "<<LITERAL_BRACE_OPEN>>")
                .replace("}}", "<<LITERAL_BRACE_CLOSE>>")

        for (info in variablesInfo) {
            val value = variables[info.name]

            if (value == null) {
                if (!info.isOptional) {
                    throw MissingVariableException(info.name)
                }
                // When optional variable is missing, replace placeholder with empty string
                // Handle possible leading/trailing space in template for cleaner result
                val optionalPlaceholder = "{${info.name}?}"
                result = result.replace(optionalPlaceholder, "")
            } else {
                val optionalPlaceholder = "{${info.name}?}"
                val requiredPlaceholder = "{${info.name}}"
                val replacement = value.asString()
                result =
                    result
                        .replace(optionalPlaceholder, replacement)
                        .replace(requiredPlaceholder, replacement)
            }
        }

        return result
            .replace("<<LITERAL_BRACE_OPEN>>", "{")
            .replace("<<LITERAL_BRACE_CLOSE>>", "}")
    }

    /**
     * Combines this template with another string.
     */
    fun append(other: String): PromptTemplate = PromptTemplate(template + other)

    /**
     * Combines this template with another template.
     */
    fun append(other: PromptTemplate): PromptTemplate = PromptTemplate(template + other.template)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PromptTemplate) return false
        return template == other.template
    }

    override fun hashCode(): Int = template.hashCode()

    override fun toString(): String = "PromptTemplate(template='$template')"

    companion object {
        // Matches {name} or {name?} while ignoring {{name}}
        private val variableRegex = "(?<!\\{)\\{([a-zA-Z0-9_]+)(\\?)?\\}(?!\\})".toRegex()
    }
}

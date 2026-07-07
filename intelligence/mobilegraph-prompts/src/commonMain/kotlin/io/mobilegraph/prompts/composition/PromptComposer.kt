package io.mobilegraph.prompts.composition

import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolDefinition
import io.mobilegraph.core.tools.asDefinition
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.UserMessage

/**
 * DSL for dynamic prompt construction.
 */
class PromptComposer {
    private val messages = mutableListOf<ChatMessage>()
    private val toolDefinitions = mutableListOf<ToolDefinition>()

    /**
     * Budget for token usage.
     */
    var tokenBudget: TokenBudget? = null

    /**
     * Adds a system message.
     */
    fun system(content: String) {
        messages.add(SystemMessage(content))
    }

    /**
     * Adds a human/user message.
     */
    fun human(content: String) {
        messages.add(UserMessage(content))
    }

    fun system(content: SystemMessage) {
        messages.add((content))
    }

    /**
     * Adds a human/user message.
     */
    fun user(content: UserMessage) {
        messages.add((content))
    }

    /**
     * Adds an assistant message.
     */
    fun assistant(content: String) {
        messages.add(AssistantMessage(content))
    }

    fun memory(
        sessionMemory: Any,
        sessionId: String,
    ) {
        // Implementation for memory injection
    }

    fun context(
        retriever: Any,
        query: String,
        maxTokens: Int = 1500,
        template: String = "Relevant information:\n{content}",
    ) {
        // Implementation for RAG context injection
    }

    fun tools(tools: List<Tool<*, *>>) {
        toolDefinitions.addAll(tools.map { it.asDefinition() })
    }

    /**
     * Finalizes the composition into a ChatPromptValue.
     */
    fun compose(): ChatPromptValue {
        // In a full implementation, this would enforce the tokenBudget
        return ChatPromptValue(messages.toList())
    }

    /**
     * Returns the tools registered in this composer.
     */
    fun getTools(): List<ToolDefinition> = toolDefinitions.toList()
}

/**
 * Represents priority and limits for tokens.
 */
data class TokenBudget(
    val total: Int,
    val priorities: Map<String, Int> = emptyMap(),
)

fun defaultPriorities() = mapOf("system" to 1, "context" to 2, "history" to 3, "human" to 4)

/**
 * Entry point for the Prompt Composer DSL.
 */
fun promptComposer(block: PromptComposer.() -> Unit): PromptComposer {
    val composer = PromptComposer()
    composer.block()
    return composer
}

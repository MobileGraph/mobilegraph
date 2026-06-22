package io.mobilegraph.prompts.chat

import io.mobilegraph.prompts.templates.PromptTemplate
import io.mobilegraph.prompts.variables.Variables

/**
 * Represents a prompt structured for chat models.
 */
data class ChatPrompt(
    val messages: List<ChatMessagePrompt>,
) {
    /**
     * Renders the chat prompt into a list of messages.
     */
    fun render(variables: Variables): List<RenderedChatMessage> = messages.map { it.render(variables) }

    /**
     * Appends another chat prompt to this one.
     */
    fun append(other: ChatPrompt): ChatPrompt = ChatPrompt(messages + other.messages)

    /**
     * Appends a message to this chat prompt.
     */
    fun append(message: ChatMessagePrompt): ChatPrompt = ChatPrompt(messages + message)

    companion object {
        fun system(template: String): ChatPrompt = ChatPrompt(listOf(ChatMessagePrompt(ChatRole.System, PromptTemplate(template))))

        fun user(template: String): ChatPrompt = ChatPrompt(listOf(ChatMessagePrompt(ChatRole.User, PromptTemplate(template))))

        fun assistant(template: String): ChatPrompt = ChatPrompt(listOf(ChatMessagePrompt(ChatRole.Assistant, PromptTemplate(template))))
    }
}

/**
 * A single message template within a chat prompt.
 */
data class ChatMessagePrompt(
    val role: ChatRole,
    val template: PromptTemplate,
) {
    fun render(variables: Variables): RenderedChatMessage = RenderedChatMessage(role, template.render(variables))
}

/**
 * A rendered chat message.
 */
data class RenderedChatMessage(
    val role: ChatRole,
    val content: String,
)

/**
 * Roles supported in chat prompts.
 */
enum class ChatRole {
    System,
    User,
    Assistant,
}

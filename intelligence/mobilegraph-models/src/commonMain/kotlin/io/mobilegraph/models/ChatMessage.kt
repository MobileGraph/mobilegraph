package io.mobilegraph.models

import kotlinx.serialization.Serializable

/**
 * Represents the synchronization state of a message.
 */
@Serializable
enum class SyncState {
    PENDING,
    SYNCED,
    FAILED,
}

/**
 * Sealed class representing different types of messages in a chat conversation.
 */
@Serializable
sealed class ChatMessage {
    abstract val content: String
    abstract val id: String
    abstract val sessionId: String
    abstract val syncState: SyncState
    abstract val timestamp: Long

    abstract fun copyWith(
        content: String = this.content,
        id: String = this.id,
        sessionId: String = this.sessionId,
        syncState: SyncState = this.syncState,
        timestamp: Long = this.timestamp,
    ): ChatMessage

    /**
     * Converts this message to the legacy Message format for internal use.
     */
    fun toMessage(): Message =
        when (this) {
            is SystemMessage -> Message(Role.System, content)
            is HumanMessage -> Message(Role.User, content)
            is AssistantMessage -> Message(Role.Assistant, content)
            is ToolMessage -> Message(Role.Tool, content)
        }

    companion object {
        /**
         * Converts a legacy Message to the new ChatMessage hierarchy.
         */
        fun fromMessage(message: Message): ChatMessage =
            when (message.role) {
                Role.System -> SystemMessage(message.content)
                Role.User -> HumanMessage(message.content)
                Role.Assistant -> AssistantMessage(message.content)
                Role.Tool -> ToolMessage(message.content, "") // Tool ID might be missing in legacy
            }
    }
}

/**
 * A message from the system, usually used to set the behavior of the assistant.
 */
@Serializable
data class SystemMessage(
    override val content: String,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.SYNCED,
    override val timestamp: Long = 0,
) : ChatMessage() {
    override fun copyWith(
        content: String,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(content = content, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message from a human user.
 */
@Serializable
data class HumanMessage(
    override val content: String,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    override fun copyWith(
        content: String,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(content = content, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message from the AI assistant.
 */
@Serializable
data class AssistantMessage(
    override val content: String,
    val toolCallId: String? = null,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    override fun copyWith(
        content: String,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(content = content, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message representing the result of a tool call.
 */
@Serializable
data class ToolMessage(
    override val content: String,
    val toolCallId: String,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    override fun copyWith(
        content: String,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(content = content, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * Represents a prompt value consisting of a list of messages.
 */
@Serializable
data class ChatPromptValue(
    val messages: List<ChatMessage>,
)

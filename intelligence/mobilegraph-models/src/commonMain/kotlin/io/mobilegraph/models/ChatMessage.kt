package io.mobilegraph.models

import kotlinx.serialization.Serializable

/**
 * Represents a part of a message's content.
 * This enables multi-modal messages containing text, images, and files.
 */
@Serializable
sealed class ContentPart {
    /**
     * Plain text content.
     */
    @Serializable
    data class Text(
        val text: String,
    ) : ContentPart()

    /**
     * Image content.
     * @property data The image data, either a URL or a Base64-encoded string.
     * @property mediaType The MIME type of the image (e.g., "image/jpeg").
     * @property bytes Raw image data. If provided, models will encode this to Base64.
     */
    @Serializable
    data class Image(
        val data: String = "",
        val mediaType: String = "image/jpeg",
        val bytes: ByteArray? = null,
    ) : ContentPart()

    /**
     * File content (e.g., PDF).
     * @property data The file data, either a URL or a Base64-encoded string.
     * @property mediaType The MIME type of the file (e.g., "application/pdf").
     */
    @Serializable
    data class File(
        val data: String,
        val mediaType: String,
    ) : ContentPart()
}

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
    /**
     * The parts making up the message content.
     */
    abstract val parts: List<ContentPart>

    /**
     * Convenience property to get the textual content of the message.
     * Combines all [ContentPart.Text] parts.
     */
    open val content: String
        get() = parts.filterIsInstance<ContentPart.Text>().joinToString("\n") { it.text }

    abstract val id: String
    abstract val sessionId: String
    abstract val syncState: SyncState
    abstract val timestamp: Long

    abstract fun copyWith(
        parts: List<ContentPart> = this.parts,
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
            is UserMessage -> Message(Role.User, content)
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
                Role.User -> UserMessage(message.content)
                Role.Assistant -> AssistantMessage(message.content)
                Role.Tool -> ToolMessage(message.content, "")
            }
    }
}

/**
 * A message from the system, usually used to set the behavior of the assistant.
 */
@Serializable
data class SystemMessage(
    override val parts: List<ContentPart>,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.SYNCED,
    override val timestamp: Long = 0,
) : ChatMessage() {
    constructor(
        content: String,
        id: String = "",
        sessionId: String = "global",
        syncState: SyncState = SyncState.SYNCED,
        timestamp: Long = 0,
    ) : this(listOf(ContentPart.Text(content)), id, sessionId, syncState, timestamp)

    override fun copyWith(
        parts: List<ContentPart>,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(parts = parts, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message from a human user.
 */
@Serializable
data class UserMessage(
    override val parts: List<ContentPart>,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    constructor(
        content: String,
        id: String = "",
        sessionId: String = "global",
        syncState: SyncState = SyncState.PENDING,
        timestamp: Long = 0,
    ) : this(listOf(ContentPart.Text(content)), id, sessionId, syncState, timestamp)

    override fun copyWith(
        parts: List<ContentPart>,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(parts = parts, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message from the AI assistant.
 */
@Serializable
data class AssistantMessage(
    override val parts: List<ContentPart>,
    val toolCallId: String? = null,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    constructor(
        content: String,
        toolCallId: String? = null,
        id: String = "",
        sessionId: String = "global",
        syncState: SyncState = SyncState.PENDING,
        timestamp: Long = 0,
    ) : this(listOf(ContentPart.Text(content)), toolCallId, id, sessionId, syncState, timestamp)

    override fun copyWith(
        parts: List<ContentPart>,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(parts = parts, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * A message representing the result of a tool call.
 */
@Serializable
data class ToolMessage(
    override val parts: List<ContentPart>,
    val toolCallId: String,
    override val id: String = "",
    override val sessionId: String = "global",
    override val syncState: SyncState = SyncState.PENDING,
    override val timestamp: Long = 0,
) : ChatMessage() {
    constructor(
        content: String,
        toolCallId: String,
        id: String = "",
        sessionId: String = "global",
        syncState: SyncState = SyncState.PENDING,
        timestamp: Long = 0,
    ) : this(listOf(ContentPart.Text(content)), toolCallId, id, sessionId, syncState, timestamp)

    override fun copyWith(
        parts: List<ContentPart>,
        id: String,
        sessionId: String,
        syncState: SyncState,
        timestamp: Long,
    ): ChatMessage = copy(parts = parts, id = id, sessionId = sessionId, syncState = syncState, timestamp = timestamp)
}

/**
 * Represents a prompt value consisting of a list of messages.
 */
@Serializable
data class ChatPromptValue(
    val messages: List<ChatMessage>,
)

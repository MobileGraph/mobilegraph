package io.mobilegraph.models

import kotlinx.serialization.Serializable

/**
 * Represents a message in a chat conversation.
 */
@Serializable
data class Message(
    val role: Role,
    val content: String,
)

/**
 * Supported message roles.
 */
@Serializable
enum class Role {
    System,
    User,
    Assistant,
    Tool,
}

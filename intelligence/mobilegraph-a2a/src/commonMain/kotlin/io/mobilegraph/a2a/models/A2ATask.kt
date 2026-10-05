package io.mobilegraph.a2a.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * An A2A Task represents a unit of work exchanged between agents.
 *
 * Tasks have a lifecycle: submitted → working → completed/failed/canceled.
 * The `input-required` state indicates the remote agent needs
 * additional information before proceeding.
 */
@Serializable
data class A2ATask(
    /** Unique task identifier. */
    val id: String = "",
    /** Context identifier for grouping related tasks into a conversation. */
    val contextId: String? = null,
    /** Current task status. */
    val status: A2ATaskStatus = A2ATaskStatus(),
    /** History of messages exchanged in this task. */
    val history: List<A2AMessage> = emptyList(),
    /** Artifacts produced by the task. */
    val artifacts: List<A2AArtifact> = emptyList(),
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

/**
 * A2A task status, combining the state enum with an optional
 * status message and timestamp.
 */
@Serializable
data class A2ATaskStatus(
    /** Current task state. */
    val state: A2ATaskState = A2ATaskState.COMPLETED,
    /** Optional status message from the agent. */
    val message: A2AMessage? = null,
    /** ISO 8601 timestamp of this status. */
    val timestamp: String? = null,
)

/**
 * A2A task lifecycle states.
 */
@Serializable(with = A2ATaskStateSerializer::class)
enum class A2ATaskState {
    SUBMITTED,
    WORKING,
    INPUT_REQUIRED,
    COMPLETED,
    FAILED,
    CANCELED,
    ;

    /** Returns `true` if this is a terminal state (completed, failed, or canceled). */
    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED || this == CANCELED
}

/**
 * Custom serializer for [A2ATaskState] supporting case-insensitive deserialization
 * (e.g., "completed", "COMPLETED", "input-required", "input_required").
 */
object A2ATaskStateSerializer : KSerializer<A2ATaskState> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.mobilegraph.a2a.models.A2ATaskState", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: A2ATaskState,
    ) {
        encoder.encodeString(
            when (value) {
                A2ATaskState.SUBMITTED -> "submitted"
                A2ATaskState.WORKING -> "working"
                A2ATaskState.INPUT_REQUIRED -> "input-required"
                A2ATaskState.COMPLETED -> "completed"
                A2ATaskState.FAILED -> "failed"
                A2ATaskState.CANCELED -> "canceled"
            },
        )
    }

    override fun deserialize(decoder: Decoder): A2ATaskState {
        val raw =
            decoder
                .decodeString()
                .trim()
                .lowercase()
                .removePrefix("task_state_")
        return when (raw) {
            "submitted" -> A2ATaskState.SUBMITTED
            "working" -> A2ATaskState.WORKING
            "input-required", "input_required", "inputrequired" -> A2ATaskState.INPUT_REQUIRED
            "completed", "success", "done" -> A2ATaskState.COMPLETED
            "failed", "failure", "error" -> A2ATaskState.FAILED
            "canceled", "cancelled" -> A2ATaskState.CANCELED
            else -> A2ATaskState.COMPLETED
        }
    }
}

/**
 * A message in the A2A conversation between agents.
 *
 * Messages contain one or more parts (text, file, data)
 * and are associated with a role (user or agent).
 */
@Serializable
data class A2AMessage(
    /** The role of the message sender. */
    val role: A2ARole,
    /** Content parts of the message. */
    val parts: List<A2APart>,
    /** Unique message identifier. */
    val messageId: String? = null,
    /** Task identifier for HITL follow-up messages. */
    val taskId: String? = null,
    /** Context identifier for grouping conversation messages. */
    val contextId: String? = null,
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

/**
 * The role of a message sender in the A2A conversation.
 *
 * Fully compliant with A2A Protocol v1.0 ("USER", "AGENT") with case-insensitive deserialization.
 */
@Serializable(with = A2ARoleSerializer::class)
enum class A2ARole {
    USER,
    AGENT,
}

/**
 * Custom serializer for [A2ARole] supporting A2A Protocol v1.0 standard uppercase values
 * ("USER", "AGENT") while accepting lowercase ("user", "agent") or prefix ("ROLE_USER") during deserialization.
 */
object A2ARoleSerializer : KSerializer<A2ARole> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.mobilegraph.a2a.models.A2ARole", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: A2ARole,
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): A2ARole {
        val raw = decoder.decodeString().trim()
        return when (raw.uppercase()) {
            "USER", "ROLE_USER" -> A2ARole.USER
            "AGENT", "ROLE_AGENT" -> A2ARole.AGENT
            else -> A2ARole.USER
        }
    }
}

/**
 * A content part within an A2A message or artifact.
 *
 * A2A supports three part types:
 * - [TextPart]: Plain text content
 * - [FilePart]: File reference (inline base64 or URI)
 * - [DataPart]: Structured JSON data
 */
@Serializable(with = A2APartSerializer::class)
sealed interface A2APart {
    /** Optional metadata for this part. */
    val metadata: JsonObject?
}

/**
 * Custom polymorphic serializer for [A2APart] supporting both explicit "type" field
 * ("text", "file", "data") and A2A v1.0 Protobuf key-presence payloads ({"text": "..."}, {"file": {...}}, {"data": {...}}).
 */
object A2APartSerializer : JsonContentPolymorphicSerializer<A2APart>(A2APart::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<A2APart> {
        val jsonObject = element.jsonObject

        // 1. Check for explicit "type" field
        val type = jsonObject["type"]?.let { if (it is JsonPrimitive) it.content else null }
        if (type != null) {
            return when (type) {
                "text" -> TextPart.serializer()
                "file" -> FilePart.serializer()
                "data" -> DataPart.serializer()
                else -> TextPart.serializer()
            }
        }

        // 2. Infer type from key presence
        return when {
            "file" in jsonObject -> FilePart.serializer()
            "data" in jsonObject -> DataPart.serializer()
            "text" in jsonObject -> TextPart.serializer()
            else -> TextPart.serializer()
        }
    }
}

/**
 * A text content part.
 */
@Serializable
@SerialName("text")
data class TextPart(
    /** The text content. */
    val text: String,
    override val metadata: JsonObject? = null,
) : A2APart

/**
 * A file content part — can be inline (base64-encoded) or referenced by URI.
 */
@Serializable
@SerialName("file")
data class FilePart(
    /** The file data. */
    val file: FileData,
    override val metadata: JsonObject? = null,
) : A2APart

/**
 * A structured data part containing arbitrary JSON.
 */
@Serializable
@SerialName("data")
data class DataPart(
    /** The structured data content. */
    val data: JsonElement,
    override val metadata: JsonObject? = null,
) : A2APart

/**
 * File data — either inline (base64) or referenced by URI.
 */
@Serializable
data class FileData(
    /** File name. */
    val name: String? = null,
    /** MIME type (e.g., "application/pdf"). */
    val mimeType: String? = null,
    /** Base64-encoded file content (for inline files). */
    val bytes: String? = null,
    /** URI reference to the file (for remote files). */
    val uri: String? = null,
)

/**
 * An artifact produced by a task.
 *
 * Artifacts represent the output/deliverables of an A2A task,
 * such as generated code, analysis reports, or data files.
 */
@Serializable
data class A2AArtifact(
    /** Unique artifact identifier. */
    val artifactId: String? = null,
    /** Human-readable name. */
    val name: String? = null,
    /** Description of what this artifact contains. */
    val description: String? = null,
    /** Content parts of the artifact. */
    val parts: List<A2APart> = emptyList(),
    /** Ordering index for multi-artifact tasks. */
    val index: Int? = null,
    /** If true, append to existing artifact content (for streaming). */
    val append: Boolean? = null,
    /** If true, this is the last chunk of the artifact (for streaming). */
    val lastChunk: Boolean? = null,
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

// --- Request/Response Parameter Types ---

/**
 * Parameters for `message/send` and `message/stream` requests.
 */
@Serializable
data class A2AMessageSendParams(
    /** The message to send to the remote agent. */
    val message: A2AMessage,
    /** Optional configuration for the request. */
    val configuration: A2AMessageSendConfiguration? = null,
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

/**
 * Configuration for a message/send request.
 */
@Serializable
data class A2AMessageSendConfiguration(
    /** Maximum number of history messages to include in the response. */
    val historyLength: Int? = null,
    /** Whether to block until the task completes. */
    val blocking: Boolean? = null,
    /** Accepted output content modes. */
    val acceptedOutputModes: List<String>? = null,
)

/**
 * Parameters for `tasks/get` requests.
 */
@Serializable
data class A2ATaskQueryParams(
    /** The task ID to query. */
    val id: String,
    /** Maximum number of history messages to include. */
    val historyLength: Int? = null,
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

/**
 * Parameters for `tasks/cancel` requests.
 */
@Serializable
data class A2ATaskCancelParams(
    /** The task ID to cancel. */
    val id: String,
    /** Arbitrary metadata. */
    val metadata: JsonObject? = null,
)

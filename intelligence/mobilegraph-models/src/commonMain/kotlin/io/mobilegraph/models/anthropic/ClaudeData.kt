package io.mobilegraph.models.anthropic

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ClaudeChatRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    @EncodeDefault(NEVER) val system: String? = null,
    @SerialName("max_tokens") val maxTokens: Int,
    @EncodeDefault(NEVER) val temperature: Double? = null,
    @SerialName("stop_sequences") @EncodeDefault(NEVER) val stopSequences: List<String>? = null,
    @EncodeDefault(NEVER) val tools: List<ClaudeTool>? = null,
    @SerialName("tool_choice") @EncodeDefault(NEVER) val toolChoice: ClaudeToolChoice? = null,
)

@Serializable
internal data class ClaudeMessage(
    val role: String,
    val content: JsonElement, // Can be String or List<ClaudeContentPart>
)

@Serializable
internal data class ClaudeContentPart(
    val type: String,
    @EncodeDefault(NEVER) val text: String? = null,
    @EncodeDefault(NEVER) val source: ClaudeImageSource? = null,
    @EncodeDefault(NEVER) val id: String? = null, // for tool_use
    @EncodeDefault(NEVER) val name: String? = null, // for tool_use
    @EncodeDefault(NEVER) val input: JsonObject? = null, // for tool_use
    @SerialName("tool_use_id") @EncodeDefault(NEVER) val toolUseId: String? = null, // for tool_result
    @EncodeDefault(NEVER) val output: String? = null, // for tool_result
)

@Serializable
internal data class ClaudeImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String,
)

@Serializable
internal data class ClaudeTool(
    val name: String,
    val description: String,
    @SerialName("input_schema") val inputSchema: JsonObject,
)

@Serializable
internal data class ClaudeToolChoice(
    val type: String,
    @EncodeDefault(NEVER) val name: String? = null,
)

@Serializable
internal data class ClaudeChatResponse(
    val id: String,
    val model: String,
    val role: String,
    val content: List<ClaudeContentPart>,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: ClaudeUsage? = null,
)

@Serializable
internal data class ClaudeUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int,
)

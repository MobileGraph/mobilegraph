package io.mobilegraph.models.openai

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @EncodeDefault(NEVER) val temperature: Double? = null,
    @SerialName("max_tokens") @EncodeDefault(NEVER) val maxTokens: Int? = null,
    @EncodeDefault(NEVER) val stop: List<String>? = null,
    val stream: Boolean = false,
    @EncodeDefault(NEVER) val tools: List<OpenAITool>? = null,
    @SerialName("tool_choice") @EncodeDefault(NEVER) val toolChoice: String? = null,
)

@Serializable
data class OpenAITool(
    val type: String = "function",
    val function: OpenAIFunction,
)

@Serializable
data class OpenAIFunction(
    val name: String,
    val description: String,
    @EncodeDefault(NEVER) val parameters: JsonObject? = null,
)

@Serializable
data class OpenAIToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAIFunctionCall,
)

@Serializable
data class OpenAIFunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
data class OpenAIMessage(
    val role: String,
    /**
     * content can be a String or a List of OpenAIContentPart.
     * We use JsonElement to handle this dynamic type.
     */
    @EncodeDefault(NEVER) val content: JsonElement? = null,
    @SerialName("tool_calls") @EncodeDefault(NEVER) val toolCalls: List<OpenAIToolCall>? = null,
    @SerialName("tool_call_id") @EncodeDefault(NEVER) val toolCallId: String? = null,
)

@Serializable
data class OpenAIContentPart(
    val type: String,
    @EncodeDefault(NEVER) val text: String? = null,
    @SerialName("image_url") @EncodeDefault(NEVER) val imageUrl: OpenAIImageUrl? = null,
)

@Serializable
data class OpenAIImageUrl(
    val url: String,
    @EncodeDefault(NEVER) val detail: String? = null,
)

@Serializable
data class OpenAIChatResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null,
)

@Serializable
data class OpenAIChoice(
    val message: OpenAIMessage? = null,
    val delta: OpenAIDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OpenAIDelta(
    val content: String? = null,
)

@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
)

@Serializable
data class OpenAIEmbeddingRequest(
    val model: String,
    val input: List<String>,
)

@Serializable
data class OpenAIEmbeddingResponse(
    val data: List<OpenAIEmbeddingData>,
    val usage: OpenAIUsage,
)

@Serializable
data class OpenAIEmbeddingData(
    val embedding: List<Float>,
    val index: Int,
)

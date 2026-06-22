package io.mobilegraph.models.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class OpenAIChatRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val tools: List<OpenAITool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
)

@Serializable
internal data class OpenAITool(
    val type: String = "function",
    val function: OpenAIFunction,
)

@Serializable
internal data class OpenAIFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null,
)

@Serializable
internal data class OpenAIToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAIFunctionCall,
)

@Serializable
internal data class OpenAIFunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
internal data class OpenAIMessage(
    val role: String,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAIToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
internal data class OpenAIChatResponse(
    val id: String,
    val choices: List<OpenAIChoice>,
    val usage: OpenAIUsage? = null,
)

@Serializable
internal data class OpenAIChoice(
    val message: OpenAIMessage? = null,
    val delta: OpenAIDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class OpenAIDelta(
    val content: String? = null,
)

@Serializable
internal data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
)

@Serializable
internal data class OpenAIEmbeddingRequest(
    val model: String,
    val input: List<String>,
)

@Serializable
internal data class OpenAIEmbeddingResponse(
    val data: List<OpenAIEmbeddingData>,
    val usage: OpenAIUsage,
)

@Serializable
internal data class OpenAIEmbeddingData(
    val embedding: List<Float>,
    val index: Int,
)

package io.mobilegraph.models.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class GeminiChatRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    val tools: List<GeminiTool>? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
internal data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null,
    @SerialName("file_data") val fileData: GeminiFileData? = null,
    @SerialName("function_call") val functionCall: GeminiFunctionCall? = null,
    @SerialName("function_response") val functionResponse: GeminiFunctionResponse? = null,
)

@Serializable
internal data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String, // Base64
)

@Serializable
internal data class GeminiFileData(
    @SerialName("mime_type") val mimeType: String,
    @SerialName("file_uri") val fileUri: String,
)

@Serializable
internal data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject,
)

@Serializable
internal data class GeminiFunctionResponse(
    val name: String,
    val response: JsonObject,
)

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    val stopSequences: List<String>? = null,
    val responseMimeType: String? = null,
)

@Serializable
internal data class GeminiTool(
    @SerialName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>,
)

@Serializable
internal data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null,
)

@Serializable
internal data class GeminiChatResponse(
    val candidates: List<GeminiCandidate>,
    val usageMetadata: GeminiUsage? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null,
)

@Serializable
internal data class GeminiUsage(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int,
)

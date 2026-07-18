package io.mobilegraph.models.google

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.tools.ToolDefinition
import io.mobilegraph.core.tools.executeFromJson
import io.mobilegraph.core.tools.toolRegistry
import io.mobilegraph.core.tools.tools
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.AuthenticationException
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.InvalidModelException
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelException
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.RateLimitException
import io.mobilegraph.models.StreamingChatModel
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.ToolMessage
import io.mobilegraph.models.Usage
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64

class GeminiChatModel(
    override val name: String = "gemini-2.5-flash-lite",
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val httpClient: HttpClient = createGeminiHttpClient(),
) : StreamingChatModel {
    private val json = Json { ignoreUnknownKeys = true }

    override fun supports(capability: Capability): Boolean =
        capability in
            setOf(
                Capability.Streaming,
                Capability.FunctionCalling,
                Capability.Vision,
            )

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput =
        try {
            val systemMessage = prompt.messages.filterIsInstance<SystemMessage>().firstOrNull()
            val otherMessages = prompt.messages.filter { it !is SystemMessage }

            val contents = otherMessages.map { it.toGemini() }
            val systemInstruction = systemMessage?.toGemini()

            val geminiRequest =
                GeminiChatRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    generationConfig =
                        GeminiGenerationConfig(
                            temperature = config?.temperature?.toDouble(),
                            maxOutputTokens = config?.maxTokens,
                            stopSequences = config?.stop,
                        ),
                    tools =
                        config?.tools?.let { tools ->
                            listOf(GeminiTool(tools.map { it.toGemini() }))
                        },
                )

            var response =
                httpClient.post("$baseUrl/models/$name:generateContent?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(geminiRequest)
                }

            if (response.status.value >= 400) {
                return ModelOutput.ErrorOutput(translateError(response))
            }

            var apiResponse: GeminiChatResponse = response.body()
            var candidate = apiResponse.candidates.first()

            // Handle Tool Calls (Simplified loop)
            val currentContents = contents.toMutableList()

            while (candidate.content.parts.any { it.functionCall != null }) {
                val modelContent = candidate.content
                currentContents.add(modelContent)

                val toolResponses =
                    modelContent.parts.mapNotNull { part ->
                        part.functionCall?.let { call ->
                            val result = fetchAndExecuteTool(call.name, call.args.toString(), context)
                            GeminiPart(
                                functionResponse =
                                    GeminiFunctionResponse(
                                        name = call.name,
                                        response =
                                            try {
                                                json.parseToJsonElement(result) as JsonObject
                                            } catch (e: Exception) {
                                                JsonObject(mapOf("result" to JsonPrimitive(result)))
                                            },
                                    ),
                            )
                        }
                    }

                currentContents.add(GeminiContent(role = "function", parts = toolResponses))

                val nextRequest = geminiRequest.copy(contents = currentContents)
                response =
                    httpClient.post("$baseUrl/models/$name:generateContent?key=$apiKey") {
                        contentType(ContentType.Application.Json)
                        setBody(nextRequest)
                    }

                if (response.status.value >= 400) {
                    return ModelOutput.ErrorOutput(translateError(response))
                }

                apiResponse = response.body()
                candidate = apiResponse.candidates.first()
            }

            val text = candidate.content.parts.joinToString("\n") { it.text ?: "" }
            ModelOutput.ChatOutput(
                message = AssistantMessage(text),
                usage =
                    apiResponse.usageMetadata?.let {
                        Usage(it.promptTokenCount, it.candidatesTokenCount, it.totalTokenCount)
                    },
            )
        } catch (e: Exception) {
            ModelOutput.ErrorOutput(e)
        }

    private suspend fun translateError(response: HttpResponse): Throwable {
        val status = response.status.value
        val body = response.bodyAsText()
        return when (status) {
            401, 403 -> AuthenticationException("Authentication failed: $body")
            429 -> RateLimitException("Rate limit exceeded: $body")
            404 -> InvalidModelException("Model not found: $body")
            else -> ModelException("API Error ($status): $body")
        }
    }

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): Flow<ChatChunk> =
        kotlinx.coroutines.flow.flow {
            val systemMessage = prompt.messages.filterIsInstance<SystemMessage>().firstOrNull()
            val otherMessages = prompt.messages.filter { it !is SystemMessage }

            val contents = otherMessages.map { it.toGemini() }
            val systemInstruction = systemMessage?.toGemini()

            val geminiRequest =
                GeminiChatRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    generationConfig =
                        GeminiGenerationConfig(
                            temperature = config?.temperature?.toDouble(),
                            maxOutputTokens = config?.maxTokens,
                            stopSequences = config?.stop,
                        ),
                    tools =
                        config?.tools?.let { tools ->
                            listOf(GeminiTool(tools.map { it.toGemini() }))
                        },
                )

            httpClient
                .preparePost("$baseUrl/models/$name:streamGenerateContent?alt=sse&key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(geminiRequest)
                }.execute { response ->
                    if (response.status.value >= 400) {
                        throw translateError(response)
                    }

                    val channel: ByteReadChannel = response.body()
                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val jsonStr = line.substring(6)
                            try {
                                val chunkResponse = json.decodeFromString<GeminiChatResponse>(jsonStr)
                                val textDelta =
                                    chunkResponse.candidates.firstOrNull()?.content?.parts?.joinToString("") {
                                        it.text ?: ""
                                    }
                                if (textDelta != null) {
                                    emit(ChatChunk(delta = textDelta))
                                }
                            } catch (e: Exception) {
                                // Skip invalid lines
                            }
                        }
                    }
                }
        }

    override fun readModelConfig(): ModelConfig? = null

    private suspend fun fetchAndExecuteTool(
        name: String,
        arguments: String,
        context: ExecutionContext,
    ): String {
        val registry = context.toolRegistry ?: MobileGraph.tools.registry()
        val tool = registry.get(name) ?: return "Error: Tool $name not found"
        return try {
            tool.executeFromJson(arguments, context)
        } catch (e: Exception) {
            "Error executing tool $name: ${e.message}"
        }
    }

    private fun ChatMessage.toGemini(): GeminiContent {
        val role =
            when (this) {
                is UserMessage -> "user"
                is AssistantMessage -> "model"
                is ToolMessage -> "function"
                is SystemMessage -> null // System is handled separately in request
            }
        return GeminiContent(
            role = role,
            parts = parts.map { it.toGemini() },
        )
    }

    private fun ContentPart.toGemini(): GeminiPart =
        when (this) {
            is ContentPart.Text -> {
                GeminiPart(text = text)
            }

            is ContentPart.Image -> {
                val base64Data =
                    if (bytes != null) {
                        Base64.Default.encode(bytes)
                    } else {
                        data
                    }
                GeminiPart(
                    inlineData =
                        GeminiInlineData(
                            mimeType = mediaType,
                            data = base64Data,
                        ),
                )
            }

            is ContentPart.File -> {
                GeminiPart(
                    inlineData = GeminiInlineData(mimeType = mediaType, data = data),
                )
            }
        }

    private fun ToolDefinition.toGemini(): GeminiFunctionDeclaration =
        GeminiFunctionDeclaration(
            name = name,
            description = description,
            parameters = parameters,
        )
}

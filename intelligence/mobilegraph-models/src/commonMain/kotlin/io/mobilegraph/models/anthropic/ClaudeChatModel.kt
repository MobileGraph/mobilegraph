package io.mobilegraph.models.anthropic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64

class ClaudeChatModel(
    override val name: String,
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val httpClient: HttpClient = createClaudeHttpClient(),
    private val capabilities: Set<Capability> = defaultCapabilities(name),
) : StreamingChatModel {
    private val json = Json { ignoreUnknownKeys = true }

    override fun supports(capability: Capability): Boolean = capability in capabilities

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput =
        try {
            val systemMessage =
                prompt.messages
                    .filterIsInstance<SystemMessage>()
                    .firstOrNull()
                    ?.content
            val otherMessages = prompt.messages.filter { it !is SystemMessage }

            val messages = otherMessages.map { it.toClaude() }

            val claudeTools = config?.tools?.map { it.toClaude() }

            val claudeRequest =
                ClaudeChatRequest(
                    model = name,
                    messages = messages,
                    system = systemMessage,
                    maxTokens = config?.maxTokens ?: 1024,
                    temperature = config?.temperature?.toDouble(),
                    stopSequences = config?.stop,
                    tools = if (claudeTools.isNullOrEmpty()) null else claudeTools,
                    toolChoice = if (claudeTools.isNullOrEmpty()) null else ClaudeToolChoice("auto"),
                )

            var response =
                httpClient.post("$baseUrl/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(claudeRequest)
                }

            if (response.status.value >= 400) {
                return ModelOutput.ErrorOutput(translateError(response))
            }

            var apiResponse: ClaudeChatResponse = response.body()

            // Handle Tool Use (Recursive loop)
            val currentMessages = messages.toMutableList()

            while (apiResponse.stopReason == "tool_use") {
                val assistantContentParts = apiResponse.content
                currentMessages.add(ClaudeMessage("assistant", JsonArray(assistantContentParts.map { json.encodeToJsonElement(it) })))

                val toolResults =
                    assistantContentParts.filter { it.type == "tool_use" }.map { toolUse ->
                        val result = fetchAndExecuteTool(toolUse.name!!, toolUse.input.toString(), context)
                        ClaudeContentPart(
                            type = "tool_result",
                            toolUseId = toolUse.id,
                            output = result, // Claude expects output as string or parts
                        )
                    }

                currentMessages.add(ClaudeMessage("user", JsonArray(toolResults.map { json.encodeToJsonElement(it) })))

                val nextRequest = claudeRequest.copy(messages = currentMessages)
                response =
                    httpClient.post("$baseUrl/messages") {
                        header("x-api-key", apiKey)
                        header("anthropic-version", "2023-06-01")
                        contentType(ContentType.Application.Json)
                        setBody(nextRequest)
                    }

                if (response.status.value >= 400) {
                    return ModelOutput.ErrorOutput(translateError(response))
                }

                apiResponse = response.body()
            }

            val text = apiResponse.content.filter { it.type == "text" }.joinToString("\n") { it.text ?: "" }
            ModelOutput.ChatOutput(
                message = AssistantMessage(text),
                usage =
                    apiResponse.usage?.let {
                        Usage(it.inputTokens, it.outputTokens, it.inputTokens + it.outputTokens)
                    },
            )
        } catch (e: Exception) {
            ModelOutput.ErrorOutput(e)
        }

    private suspend fun translateError(response: HttpResponse): Throwable {
        val status = response.status.value
        val body = response.bodyAsText()
        return when (status) {
            401 -> AuthenticationException("Authentication failed: $body")
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
            val systemMessage =
                prompt.messages
                    .filterIsInstance<SystemMessage>()
                    .firstOrNull()
                    ?.content
            val otherMessages = prompt.messages.filter { it !is SystemMessage }

            val messages = otherMessages.map { it.toClaude() }

            val claudeTools = config?.tools?.map { it.toClaude() }

            val claudeRequest =
                ClaudeChatRequest(
                    model = name,
                    messages = messages,
                    system = systemMessage,
                    maxTokens = config?.maxTokens ?: 1024,
                    temperature = config?.temperature?.toDouble(),
                    stopSequences = config?.stop,
                    tools = if (claudeTools.isNullOrEmpty()) null else claudeTools,
                    toolChoice = if (claudeTools.isNullOrEmpty()) null else ClaudeToolChoice("auto"),
                    stream = true,
                )

            httpClient
                .preparePost("$baseUrl/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(claudeRequest)
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
                                val event = json.decodeFromString<ClaudeStreamEvent>(jsonStr)
                                when (event.type) {
                                    "content_block_delta" -> {
                                        val delta = event.delta?.text
                                        if (delta != null) {
                                            emit(ChatChunk(delta = delta))
                                        }
                                    }

                                    "message_delta" -> {
                                        val finishReason = event.delta?.stopReason
                                        if (finishReason != null) {
                                            emit(ChatChunk(delta = "", finishReason = finishReason))
                                        }
                                    }
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

    private suspend fun ChatMessage.toClaude(): ClaudeMessage {
        val role =
            when (this) {
                is UserMessage -> "user"

                is AssistantMessage -> "assistant"

                is ToolMessage -> "user"

                // Claude treats tool results as a user message following a tool_use
                is SystemMessage -> "user" // Should not happen with filter
            }

        return if (parts.size == 1 && parts.first() is ContentPart.Text) {
            ClaudeMessage(role, JsonPrimitive(content))
        } else {
            val partsJson = parts.map { it.toClaude() }
            ClaudeMessage(role, JsonArray(partsJson))
        }
    }

    private fun ContentPart.toClaude(): JsonElement =
        when (this) {
            is ContentPart.Text -> {
                json.encodeToJsonElement(ClaudeContentPart(type = "text", text = text))
            }

            is ContentPart.Image -> {
                val base64Data =
                    if (bytes != null) {
                        Base64.Default.encode(bytes)
                    } else {
                        data
                    }
                json.encodeToJsonElement(
                    ClaudeContentPart(
                        type = "image",
                        source =
                            ClaudeImageSource(
                                type = "base64",
                                mediaType = mediaType,
                                data = base64Data,
                            ),
                    ),
                )
            }

            is ContentPart.File -> {
                json.encodeToJsonElement(ClaudeContentPart(type = "text", text = "[File: $mediaType]"))
            }
        }

    private fun ToolDefinition.toClaude(): ClaudeTool {
        val schema = parameters ?: JsonObject(emptyMap())
        // Anthropic requires the root of the input_schema to have a "type": "object" field.
        val compliantSchema =
            if (!schema.containsKey("type")) {
                val newMap = schema.toMutableMap()
                newMap["type"] = JsonPrimitive("object")
                if (!schema.containsKey("properties")) {
                    newMap["properties"] = JsonObject(emptyMap())
                }
                JsonObject(newMap)
            } else {
                schema
            }

        return ClaudeTool(
            name = name,
            description = description,
            inputSchema = compliantSchema,
        )
    }

    companion object {
        fun defaultCapabilities(modelName: String): Set<Capability> {
            val caps =
                mutableSetOf(
                    Capability.Streaming,
                    Capability.FunctionCalling,
                )

            // Most Claude 3+ models support vision
            if (modelName.contains("claude-3")) {
                caps.add(Capability.Vision)
            }

            return caps
        }
    }
}

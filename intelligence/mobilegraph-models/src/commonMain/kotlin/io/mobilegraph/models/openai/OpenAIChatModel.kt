package io.mobilegraph.models.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readLine
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.lifecycle.LifecycleState
import io.mobilegraph.core.tools.ToolDefinition
import io.mobilegraph.core.tools.executeFromJson
import io.mobilegraph.core.tools.toolRegistry
import io.mobilegraph.core.tools.tools
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.AuthenticationException
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ChatRequest
import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.InvalidModelException
import io.mobilegraph.models.Message
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelException
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.RateLimitException
import io.mobilegraph.models.Role
import io.mobilegraph.models.StreamingChatModel
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.ToolMessage
import io.mobilegraph.models.Usage
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.io.encoding.Base64

open class OpenAIChatModel(
    override val name: String,
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
    private val capabilities: Set<Capability> = defaultCapabilities(name),
) : StreamingChatModel {
    protected val json = Json { ignoreUnknownKeys = true }

    override fun supports(capability: Capability): Boolean = capability in capabilities

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput =
        try {
            // Graceful Lifecycle Check
            if (context.lifecycleState == LifecycleState.Background) {
                println("OpenAIChatModel: Executing in background. Consider using a low-priority configuration.")
            }

            val messages = prompt.messages.map { it.toOpenAI() }
            val openAIRequest =
                OpenAIChatRequest(
                    model = name,
                    messages = messages,
                    temperature = config?.temperature?.toDouble(),
                    maxTokens = config?.maxTokens,
                    stop = if (config?.stop.isNullOrEmpty()) null else config?.stop,
                    tools = config?.tools?.map { it.toOpenAI() },
                    toolChoice = config?.toolChoice,
                )

            var response =
                httpClient
                    .post("$baseUrl/chat/completions") {
                        header("Authorization", "Bearer $apiKey")
                        contentType(ContentType.Application.Json)
                        setBody(openAIRequest)
                    }

            if (response.status.value >= 400) {
                return ModelOutput.ErrorOutput(translateError(response))
            }

            var apiResponse: OpenAIChatResponse = response.body()
            var choice = apiResponse.choices.first()

            // Handle Tool Calls if any (Simple sequential execution)
            val currentMessages = messages.toMutableList()

            while (choice.message?.toolCalls != null) {
                val assistantMsg = choice.message!!
                currentMessages.add(assistantMsg)

                for (toolCall in assistantMsg.toolCalls!!) {
                    val toolName = toolCall.function.name
                    val toolArgs = toolCall.function.arguments
                    currentMessages.add(
                        OpenAIMessage(
                            role = "tool",
                            content = JsonPrimitive(fetchAndExecuteTool(toolName, toolArgs, context)),
                            toolCallId = toolCall.id,
                        ),
                    )
                }

                // Re-invoke with tool results
                val nextRequest = openAIRequest.copy(messages = currentMessages)
                response =
                    httpClient
                        .post("$baseUrl/chat/completions") {
                            header("Authorization", "Bearer $apiKey")
                            contentType(ContentType.Application.Json)
                            setBody(nextRequest)
                        }

                if (response.status.value >= 400) {
                    return ModelOutput.ErrorOutput(translateError(response))
                }

                apiResponse = response.body()
                choice = apiResponse.choices.first()
            }

            val content = extractContent(choice.message)

            ModelOutput.ChatOutput(
                message = AssistantMessage(content),
                usage = apiResponse.usage?.toUsage(),
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

    protected fun extractContent(message: OpenAIMessage?): String =
        when (val c = message?.content) {
            is JsonPrimitive -> {
                c.content
            }

            is JsonArray -> {
                c
                    .filterIsInstance<JsonObject>()
                    .mapNotNull { it["text"] as? JsonPrimitive }
                    .joinToString("") { it.content }
            }

            else -> {
                ""
            }
        }

    protected suspend fun fetchAndExecuteTool(
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

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): Flow<ChatChunk> =
        flow {
            val messages = prompt.messages.map { it.toOpenAI() }
            val openAIRequest =
                OpenAIChatRequest(
                    model = name,
                    messages = messages,
                    temperature = config?.temperature?.toDouble(),
                    maxTokens = config?.maxTokens,
                    stop = if (config?.stop.isNullOrEmpty()) null else config?.stop,
                    stream = true,
                    tools = config?.tools?.map { it.toOpenAI() },
                    toolChoice = config?.toolChoice,
                )

            httpClient
                .post("$baseUrl/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(openAIRequest)
                }.body<io.ktor.utils.io.ByteReadChannel>()
                .let { channel ->
                    while (!channel.isClosedForRead) {
                        val line = channel.readLine() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ")
                            if (data == "[DONE]") break

                            val chunk = json.decodeFromString<OpenAIChatResponse>(data)

                            val choice = chunk.choices.firstOrNull()
                            if (choice != null) {
                                emit(
                                    ChatChunk(
                                        delta = choice.delta?.content ?: "",
                                        finishReason = choice.finishReason,
                                    ),
                                )
                            }
                        }
                    }
                }
        }

    protected fun ChatRequest.toOpenAI(
        modelName: String,
        stream: Boolean,
    ): OpenAIChatRequest =
        OpenAIChatRequest(
            model = modelName,
            messages = messages.map { it.toOpenAI() },
            temperature = temperature,
            maxTokens = maxTokens,
            stop = stop,
            stream = stream,
            tools = tools?.map { it.toOpenAI() },
            toolChoice = toolChoice,
        )

    protected fun ToolDefinition.toOpenAI(): OpenAITool =
        OpenAITool(
            function =
                OpenAIFunction(
                    name = name,
                    description = description,
                    parameters = parameters,
                ),
        )

    protected fun Message.toOpenAI(): OpenAIMessage =
        OpenAIMessage(
            role = role.name.lowercase(),
            content = JsonPrimitive(content),
            toolCallId = if (this.role == Role.Tool) "TODO_ID" else null, // Internal Message needs toolCallId
        )

    protected fun ChatMessage.toOpenAI(): OpenAIMessage =
        when (this) {
            is SystemMessage -> {
                OpenAIMessage("system", JsonPrimitive(content))
            }

            is UserMessage -> {
                if (parts.size == 1 && parts.first() is ContentPart.Text) {
                    OpenAIMessage("user", JsonPrimitive(content))
                } else {
                    val partsJson = parts.map { it.toOpenAI() }
                    OpenAIMessage("user", JsonArray(partsJson))
                }
            }

            is AssistantMessage -> {
                OpenAIMessage(
                    "assistant",
                    JsonPrimitive(content),
                    toolCalls = null,
                    toolCallId = toolCallId,
                )
            }

            is ToolMessage -> {
                OpenAIMessage("tool", JsonPrimitive(content), toolCallId = toolCallId)
            }
        }

    protected fun ContentPart.toOpenAI(): JsonElement =
        when (this) {
            is ContentPart.Text -> {
                json.encodeToJsonElement(
                    OpenAIContentPart(type = "text", text = text),
                )
            }

            is ContentPart.Image -> {
                val url =
                    if (bytes != null) {
                        val base64 = Base64.Default.encode(bytes)
                        "data:$mediaType;base64,$base64"
                    } else {
                        data
                    }
                json.encodeToJsonElement(
                    OpenAIContentPart(
                        type = "image_url",
                        imageUrl = OpenAIImageUrl(url = url),
                    ),
                )
            }

            is ContentPart.File -> {
                json.encodeToJsonElement(
                    OpenAIContentPart(type = "text", text = "[File: $mediaType]"),
                )
            }
        }

    protected fun OpenAIMessage.toMessage(): Message =
        Message(
            role = Role.entries.first { it.name.lowercase() == role },
            content =
                when (content) {
                    is JsonPrimitive -> {
                        content.content
                    }

                    is JsonArray -> {
                        content
                            .filterIsInstance<JsonObject>()
                            .mapNotNull { (it["text"] as? JsonPrimitive)?.content }
                            .joinToString("")
                    }

                    else -> {
                        ""
                    }
                },
        )

    protected fun OpenAIUsage.toUsage(): Usage =
        Usage(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
        )

    override fun readModelConfig(): ModelConfig? = null

    companion object {
        /**
         * Default capabilities for OpenAI models based on their name.
         */
        fun defaultCapabilities(modelName: String): Set<Capability> {
            val caps =
                mutableSetOf(
                    Capability.Streaming,
                    Capability.FunctionCalling,
                )

            if (modelName.contains("gpt-4o") || modelName.contains("gpt-4-turbo")) {
                caps.add(Capability.Vision)
                caps.add(Capability.StructuredOutput)
            }

            if (modelName.startsWith("o1") || modelName.startsWith("o3")) {
                caps.add(Capability.Reasoning)
                caps.add(Capability.Vision)
            }

            return caps
        }
    }
}

package io.mobilegraph.models.openai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readLine
import io.ktor.utils.io.readUTF8Line
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolDefinition
import io.mobilegraph.core.tools.executeFromJson
import io.mobilegraph.core.tools.toolRegistry
import io.mobilegraph.core.tools.tools
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ChatRequest
import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.Message
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.Role
import io.mobilegraph.models.StreamingChatModel
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.ToolMessage
import io.mobilegraph.models.Usage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OpenAIChatModel(
    override val name: String = "gpt-4o",
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
) : StreamingChatModel {
    private val json = Json { ignoreUnknownKeys = true }

    override fun supports(capability: Capability): Boolean =
        capability in
            setOf(
                Capability.Streaming,
                Capability.FunctionCalling,
                Capability.StructuredOutput,
                Capability.Vision,
            )

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput =
        try {
            val messages = prompt.messages.map { it.toOpenAI() }
            val request =
                ChatRequest(
                    messages = emptyList(), // Not used anymore as we map directly to OpenAIMessage
                    temperature = config?.temperature?.toDouble(),
                    maxTokens = config?.maxTokens,
                    stop = config?.stop,
                    tools = config?.tools,
                    toolChoice = config?.toolChoice,
                )
            val openAIRequest = request.toOpenAI(name, stream = false).copy(messages = messages)

            var response: OpenAIChatResponse =
                httpClient
                    .post("$baseUrl/chat/completions") {
                        header("Authorization", "Bearer $apiKey")
                        contentType(ContentType.Application.Json)
                        setBody(openAIRequest)
                    }.body()

            var choice = response.choices.first()

            // Handle Tool Calls if any (Simple sequential execution)
            val currentMessages = messages.toMutableList()

            while (choice.message?.toolCalls != null) {
                val assistantMsg = choice.message
                currentMessages.add(assistantMsg)

                for (toolCall in assistantMsg.toolCalls) {
                    val toolName = toolCall.function.name
                    val toolArgs = toolCall.function.arguments
                    currentMessages.add(
                        OpenAIMessage(
                            role = "tool",
                            content = fetchAndExecuteTool(toolName, toolArgs, context),
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
                        }.body()
                choice = response.choices.first()
            }

            val content = choice.message?.content ?: ""
            ModelOutput.ChatOutput(
                message = AssistantMessage(content),
                usage = response.usage?.toUsage(),
            )
        } catch (e: Exception) {
            ModelOutput.ErrorOutput(e)
        }

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

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): Flow<ChatChunk> =
        flow {
            val messages = prompt.messages.map { it.toMessage() }
            val request =
                ChatRequest(
                    messages = messages,
                    temperature = config?.temperature?.toDouble(),
                    maxTokens = config?.maxTokens,
                    stop = config?.stop,
                    tools = config?.tools,
                    toolChoice = config?.toolChoice,
                )
            val openAIRequest = request.toOpenAI(name, stream = true)

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

    private fun ChatRequest.toOpenAI(
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

    private fun ToolDefinition.toOpenAI(): OpenAITool =
        OpenAITool(
            function =
                OpenAIFunction(
                    name = name,
                    description = description,
                    parameters = parameters,
                ),
        )

    private fun Message.toOpenAI(): OpenAIMessage =
        OpenAIMessage(
            role = role.name.lowercase(),
            content = content,
            toolCallId = if (this.role == Role.Tool) "TODO_ID" else null, // Internal Message needs toolCallId
        )

    private fun ChatMessage.toOpenAI(): OpenAIMessage =
        when (this) {
            is SystemMessage -> OpenAIMessage("system", content)
            is HumanMessage -> OpenAIMessage("user", content)
            is AssistantMessage -> OpenAIMessage("assistant", content, toolCalls = null, toolCallId = toolCallId)
            is ToolMessage -> OpenAIMessage("tool", content, toolCallId = toolCallId)
        }

    private fun OpenAIMessage.toMessage(): Message =
        Message(
            role = Role.entries.first { it.name.lowercase() == role },
            content = content ?: "",
        )

    private fun OpenAIUsage.toUsage(): Usage =
        Usage(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens,
        )
}

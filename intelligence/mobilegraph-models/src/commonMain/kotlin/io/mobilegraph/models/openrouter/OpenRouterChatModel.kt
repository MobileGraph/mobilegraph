package io.mobilegraph.models.openrouter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.openai.OpenAIChatModel
import io.mobilegraph.models.openai.OpenAIChatRequest
import io.mobilegraph.models.openai.OpenAIChatResponse
import io.mobilegraph.models.openai.OpenAIMessage
import io.mobilegraph.models.openai.createOpenAIHttpClient
import kotlinx.serialization.json.JsonPrimitive

/**
 * Chat model implementation for OpenRouter.
 * Inherits most logic from [OpenAIChatModel] as the APIs are highly compatible.
 */
class OpenRouterChatModel(
    override val name: String, // e.g. "anthropic/claude-3.5-sonnet"
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1",
    private val httpClient: HttpClient = createOpenAIHttpClient(),
    private val capabilities: Set<Capability> = OpenAIChatModel.defaultCapabilities(name),
) : OpenAIChatModel(name, apiKey, baseUrl, httpClient, capabilities) {
    override fun supports(capability: Capability): Boolean = super.supports(capability) || capability == Capability.Vision

    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: ExecutionContext,
    ): ModelOutput =
        try {
            val messages = prompt.messages.map { it.toOpenAI() }
            val openAIRequest =
                OpenAIChatRequest(
                    model = name,
                    messages = messages,
                    temperature = config?.temperature?.toDouble(),
                    maxTokens = config?.maxTokens,
                    stop = if (config?.stop.isNullOrEmpty()) null else config?.stop,
                    tools = config?.tools?.map { it.toOpenAI() },
                )

            var response =
                httpClient
                    .post("$baseUrl/chat/completions") {
                        header("Authorization", "Bearer $apiKey")
                        header("HTTP-Referer", "https://mobilegraph.io")
                        header("X-Title", "MobileGraph SDK")
                        contentType(ContentType.Application.Json)
                        setBody(openAIRequest)
                    }

            if (response.status.value >= 400) {
                // Use a translation method if available, or generic exception
                // For now, using the parent's translate logic if possible or re-implementing
                throw io.mobilegraph.models.ModelException("OpenRouter API Error (${response.status.value}): ${response.body<String>()}")
            }

            var apiResponse: OpenAIChatResponse = response.body()
            var choice = apiResponse.choices.first()

            // Handle Tool Calls (Recursive)
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

                val nextRequest = openAIRequest.copy(messages = currentMessages)
                response =
                    httpClient
                        .post("$baseUrl/chat/completions") {
                            header("Authorization", "Bearer $apiKey")
                            header("HTTP-Referer", "https://mobilegraph.io")
                            header("X-Title", "MobileGraph SDK")
                            contentType(ContentType.Application.Json)
                            setBody(nextRequest)
                        }

                if (response.status.value >= 400) {
                    throw io.mobilegraph.models.ModelException(
                        "OpenRouter API Error (${response.status.value}): ${response.body<String>()}",
                    )
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
}

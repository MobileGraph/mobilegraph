package io.mobilegraph.models.openai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolMetadata
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenAIChatModelTest {
    private class SimpleToolRegistry : ToolRegistry {
        private val tools = mutableMapOf<String, Tool<*, *>>()

        override fun register(tool: Tool<*, *>) {
            tools[tool.metadata.name] = tool
        }

        override fun get(name: String): Tool<*, *>? = tools[name]

        override fun getAll(): List<Tool<*, *>> = tools.values.toList()
    }

    @Test
    fun testInvoke() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            """
                            {
                              "id": "chatcmpl-123",
                              "choices": [
                                {
                                  "index": 0,
                                  "message": {
                                    "role": "assistant",
                                    "content": "Hello! How can I help you today?"
                                  },
                                  "finish_reason": "stop"
                                }
                              ],
                              "usage": {
                                "prompt_tokens": 9,
                                "completion_tokens": 12,
                                "total_tokens": 21
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = createOpenAIHttpClient(mockEngine)
            val model = OpenAIChatModel(apiKey = "test-key", httpClient = client)
            val context =
                SimpleExecutionContext(
                    traceId = TraceId("test-trace"),
                    requestId = RequestId("test-request"),
                )
            val prompt =
                ChatPromptValue(
                    messages = listOf(HumanMessage("Hi")),
                )

            val response = model.invoke(prompt, null, context) as ModelOutput.ChatOutput

            assertEquals("Hello! How can I help you today?", response.message.content)
            assertEquals(21, response.usage?.totalTokens)
        }

    @Test
    fun testToolCallingLoop() =
        runTest {
            var callCount = 0
            val mockEngine =
                MockEngine { request ->
                    callCount++
                    when (callCount) {
                        1 -> {
                            respond(
                                content =
                                    """
                                    {
                                      "id": "chatcmpl-tool",
                                      "choices": [
                                        {
                                          "index": 0,
                                          "message": {
                                            "role": "assistant",
                                            "content": null,
                                            "tool_calls": [
                                              {
                                                "id": "call_123",
                                                "type": "function",
                                                "function": {
                                                  "name": "get_weather",
                                                  "arguments": "{\"location\": \"London\"}"
                                                }
                                              }
                                            ]
                                          },
                                          "finish_reason": "tool_calls"
                                        }
                                      ]
                                    }
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }

                        2 -> {
                            respond(
                                content =
                                    """
                                    {
                                      "id": "chatcmpl-final",
                                      "choices": [
                                        {
                                          "index": 0,
                                          "message": {
                                            "role": "assistant",
                                            "content": "The weather in London is 20°C."
                                          },
                                          "finish_reason": "stop"
                                        }
                                      ]
                                    }
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        }

                        else -> {
                            error("Unexpected call")
                        }
                    }
                }

            val client = createOpenAIHttpClient(mockEngine)
            val model = OpenAIChatModel(apiKey = "test-key", httpClient = client)

            val weatherTool =
                object : Tool<String, String> {
                    override val metadata = ToolMetadata("get_weather", "desc")

                    override suspend fun invoke(
                        input: String,
                        context: ExecutionContext,
                    ): String = "20°C"
                }

            val registry = SimpleToolRegistry()
            registry.register(weatherTool)

            val context =
                SimpleExecutionContext(
                    traceId = TraceId("t1"),
                    requestId = RequestId("r1"),
                    componentProvider =
                        object : io.mobilegraph.core.registry.ComponentProvider {
                            override fun <T : Any> getComponent(clazz: kotlin.reflect.KClass<T>): T? {
                                @Suppress("UNCHECKED_CAST")
                                return if (clazz == ToolRegistry::class) registry as T else null
                            }
                        },
                )

            val prompt = ChatPromptValue(listOf(HumanMessage("What is the weather in London?")))
            val response = model.invoke(prompt, null, context) as ModelOutput.ChatOutput

            assertEquals("The weather in London is 20°C.", response.message.content)
            assertEquals(2, callCount)
        }
}

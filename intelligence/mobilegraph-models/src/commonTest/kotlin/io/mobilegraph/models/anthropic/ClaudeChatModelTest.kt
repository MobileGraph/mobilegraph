package io.mobilegraph.models.anthropic

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.models.AuthenticationException
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaudeChatModelTest {
    @Test
    fun testSuccessfulInvoke() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            """
                            {
                              "id": "msg_123",
                              "type": "message",
                              "role": "assistant",
                              "content": [
                                {
                                  "type": "text",
                                  "text": "Hello from Claude"
                                }
                              ],
                              "model": "claude-3-5-sonnet-20240620",
                              "stop_reason": "end_turn",
                              "usage": {
                                "input_tokens": 10,
                                "output_tokens": 5
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }

            val client = createClaudeHttpClient(mockEngine)
            val model = ClaudeChatModel(apiKey = "key", name = "sonnet-3-5", httpClient = client)

            val result = model.invoke(ChatPromptValue(listOf(UserMessage("hi"))))

            assertTrue(result is ModelOutput.ChatOutput)
            assertEquals("Hello from Claude", result.message.content)
            assertEquals(15, result.usage?.totalTokens)
        }

    @Test
    fun testAuthError() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "Invalid API Key",
                        status = HttpStatusCode.Unauthorized,
                    )
                }

            val client = createClaudeHttpClient(mockEngine)
            val model = ClaudeChatModel(apiKey = "invalid", name = "sonnet-3-5", httpClient = client)

            val result = model.invoke(ChatPromptValue(listOf(UserMessage("hi"))))

            assertTrue(result is ModelOutput.ErrorOutput)
            assertTrue(result.error is AuthenticationException)
        }
}

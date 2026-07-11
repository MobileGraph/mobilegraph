package io.mobilegraph.models.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.RateLimitException
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiChatModelTest {
    @Test
    fun testSuccessfulInvoke() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content =
                            """
                            {
                              "candidates": [
                                {
                                  "content": {
                                    "parts": [{ "text": "Hello from Gemini" }],
                                    "role": "model"
                                  },
                                  "finishReason": "STOP"
                                }
                              ],
                              "usageMetadata": {
                                "promptTokenCount": 10,
                                "candidatesTokenCount": 5,
                                "totalTokenCount": 15
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }

            val client = createGeminiHttpClient(mockEngine)
            val model = GeminiChatModel(apiKey = "key", httpClient = client)

            val result = model.invoke(ChatPromptValue(listOf(UserMessage("hi"))))

            assertTrue(result is ModelOutput.ChatOutput)
            assertEquals("Hello from Gemini", result.message.content)
            assertEquals(15, result.usage?.totalTokens)
        }

    @Test
    fun testRateLimitError() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "Quota exceeded",
                        status = HttpStatusCode.TooManyRequests,
                    )
                }

            val client = createGeminiHttpClient(mockEngine)
            val model = GeminiChatModel(apiKey = "key", httpClient = client)

            val result = model.invoke(ChatPromptValue(listOf(UserMessage("hi"))))

            assertTrue(result is ModelOutput.ErrorOutput)
            assertTrue(result.error is RateLimitException)
        }
}

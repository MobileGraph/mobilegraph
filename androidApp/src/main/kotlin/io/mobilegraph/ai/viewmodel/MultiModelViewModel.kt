package io.mobilegraph.ai.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mobilegraph.ai.ApplicationLogger
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.ai.utils.ImageResizer
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import io.mobilegraph.models.facade.claude
import io.mobilegraph.models.facade.deepseek
import io.mobilegraph.models.facade.gemini
import io.mobilegraph.models.facade.huggingface
import io.mobilegraph.models.facade.models
import io.mobilegraph.models.facade.openai
import io.mobilegraph.models.facade.openrouter
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.middleware.LoggingMiddleware
import io.mobilegraph.models.middleware.ToolSelectionMiddleware
import io.mobilegraph.tools.facade.withTools
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating Multi-Model support and Multi-Modal (Vision) content.
 */
class MultiModelViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var responseText by mutableStateOf("")
    var selectedProvider by mutableStateOf("openai")
    var selectedImageBytes by mutableStateOf<ByteArray?>(null)
    val providers =
        mapOf(
            "OpenAi" to "gpt-4o-mini",
            "Gemini" to "gemini-2.5-flash-lite",
            "Anthropic" to "claude-3-5-sonnet-20241022",
            "OpenRouter" to "poolside/laguna-xs-2.1:free",
            "HuggingFace" to "meta-llama/Llama-3.2-1B-Instruct",
            "DeepSeek" to "deepseek-chat",
        )
    private var isInitialized = false

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize {
            // Add a global tool for testing
            withTools {
                register(WeatherTool())
            }

            withModels {
                // Register multiple providers with ToolSelectionMiddleware
                openai(apiKey = BuildConfig.OPEN_AI_API_KEY, providers["OpenAi"]!!) {
                    isDefault = true
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }

                // Add Gemini if key is available
                gemini(apiKey = BuildConfig.GEMINI_API_KEY, name = providers["Gemini"]!!) {
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }

                // Add Claude if key is available
                claude(apiKey = BuildConfig.ANTHROPIC_API_KEY, name = providers["Anthropic"]!!) {
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }

                // Add OpenRouter
                openrouter(apiKey = BuildConfig.OPEN_ROUTER_API_KEY, name = providers["OpenRouter"]!!) {
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }

                // Add HuggingFace
                huggingface(apiKey = "YOUR_HF_TOKEN", name = providers["HuggingFace"]!!) {
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }

                // Add DeepSeek
                deepseek(apiKey = BuildConfig.DEEP_SEEK_API_KEY, name = providers.get("DeepSeek")!!) {
                    middleware {
                        +LoggingMiddleware(ApplicationLogger())
                        +ToolSelectionMiddleware()
                    }
                }
            }
        }
    }

    fun runQuery(query: String) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Invoking $selectedProvider..."
            responseText = ""

            try {
                val model = MobileGraph.instance.models.chat(selectedProvider)

                // Build a Multi-Modal message if image is provided
                val parts = mutableListOf<ContentPart>(ContentPart.Text(query))

                // Priority: Selected bytes from gallery > URL
                if (selectedImageBytes != null) {
                    val downscaled = ImageResizer.downscaleToMax(selectedImageBytes!!)
                    parts.add(ContentPart.Image(bytes = downscaled))
                }

                val prompt = ChatPromptValue(listOf(UserMessage(parts)))

                val context =
                    SimpleExecutionContext(
                        traceId = TraceId("multi-model-${Random.nextInt()}"),
                        requestId = RequestId("req-${Random.nextInt()}"),
                    )

                val output = model.invoke(prompt, context = context)

                when (output) {
                    is ModelOutput.ChatOutput -> {
                        responseText = output.message.content
                        uiState = "Success"
                    }

                    is ModelOutput.ErrorOutput -> {
                        responseText = "Error: ${output.error.message}"
                        uiState = "Failed"
                    }
                }
            } catch (e: Exception) {
                responseText = "Exception: ${e.message}"
                uiState = "Error"
            } finally {
                isLoading = false
            }
        }
    }
}

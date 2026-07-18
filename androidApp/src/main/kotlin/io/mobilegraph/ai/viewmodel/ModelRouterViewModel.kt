package io.mobilegraph.ai.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mobilegraph.ai.BuildConfig
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ContentPart
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import io.mobilegraph.models.facade.claude
import io.mobilegraph.models.facade.gemini
import io.mobilegraph.models.facade.models
import io.mobilegraph.models.facade.openai
import io.mobilegraph.models.facade.router
import io.mobilegraph.models.facade.withModels
import io.mobilegraph.models.routing.hasImages
import io.mobilegraph.models.routing.promptLength
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel demonstrating Intelligent Model Routing.
 */
class ModelRouterViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var responseText by mutableStateOf("")
    var routedModelName by mutableStateOf("")

    private var isInitialized = false

    fun initializeSdk(context: Context) {
        if (isInitialized) return
        isInitialized = true

        MobileGraph.initialize {
            withModels {
                // 1. Setup individual providers
                openai(apiKey = BuildConfig.OPEN_AI_API_KEY, "gpt-4o-mini")

                // Assuming keys are provided for demo purposes
                gemini(apiKey = BuildConfig.GEMINI_API_KEY, name = "gemini-2.5-flash-lite")
                claude(apiKey = BuildConfig.ANTHROPIC_API_KEY, name = "claude-sonnet-4-5-20250929")

                // 2. Define the Intelligent Router
                router("smart-assistant") {
                    // Policy 1: If input contains images, use GPT-4o (Vision)
                    policy {
                        condition { it.hasImages }
                        use("gpt-4o-mini")
                    }

                    // Policy 2: If the prompt is very long, use Gemini (Large Context)
                    policy {
                        condition { it.promptLength > 500 }
                        use("gemini-2.5-flash-lite")
                    }

                    // Default: Use Claude for general reasoning
                    default("claude-sonnet-4-5-20250929")
                }
            }
        }
    }

    fun runSmartQuery(
        query: String,
        imageUrl: String? = null,
    ) {
        viewModelScope.launch {
            isLoading = true
            uiState = "Routing query..."
            responseText = ""
            routedModelName = ""

            try {
                // We invoke the ROUTER, not a specific model
                val router = MobileGraph.instance.models.chat("smart-assistant")

                val parts = mutableListOf<ContentPart>(ContentPart.Text(query))
                if (!imageUrl.isNullOrBlank()) {
                    parts.add(ContentPart.Image(data = imageUrl))
                }

                val prompt = ChatPromptValue(listOf(UserMessage(parts)))
                val context =
                    SimpleExecutionContext(
                        traceId = TraceId("router-test-${Random.nextInt()}"),
                        requestId = RequestId("req-${Random.nextInt()}"),
                    )

                // The router internally decides which model to call
                val output = router.invoke(prompt, context = context)

                when (output) {
                    is ModelOutput.ChatOutput -> {
                        responseText = output.message.content
                        // In a real app, we might want to expose which model was used in metadata
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

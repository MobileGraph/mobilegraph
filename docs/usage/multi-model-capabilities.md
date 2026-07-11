# Multi-Model Capabilities

MobileGraph provides a unified, cloud-first ecosystem that abstracts the complexities of multiple AI providers. This allows you to build resilient apps that can "switch brains" dynamically based on cost, performance, or availability.

## 1. Supported Cloud Providers

The SDK currently supports the following providers through a standardized interface:

| Provider | Default Model | Native Vision | Capabilities |
| :--- | :--- | :---: | :--- |
| **OpenAI** | `gpt-4o` | ✅ | Function Calling, Streaming, Structured Output |
| **Google Gemini** | `gemini-1.5-flash` | ✅ | Large Context, Native Tool Use |
| **Anthropic Claude** | `claude-3-5-sonnet` | ✅ | Complex Reasoning, Tool Use |
| **OpenRouter** | Customizable | ✅ | Unified Access to 100+ Models |
| **DeepSeek** | `deepseek-chat` | ❌ | High Performance, Cost-Effective |
| **Hugging Face** | Customizable | ❌ | Open-Source Models (Llama, Mistral) |

## 2. Multi-Modal Vision

MobileGraph standardizes vision inputs. You can send images to any vision-capable model using the `ContentPart` API.

```kotlin
val message = UserMessage(
    parts = listOf(
        ContentPart.Text("Analyze this photo:"),
        ContentPart.Image(bytes = imageBytes, mediaType = "image/jpeg")
    )
)
```

The SDK automatically handles provider-specific encoding:
- **OpenAI/OpenRouter**: Encodes to Base64 Data URIs.
- **Gemini**: Maps to `inline_data`.
- **Claude**: Maps to Anthropic's `base64` source structure.

## 3. Intelligent Model Router

The **Model Router** is the orchestration brain. It allows you to define policies to select models automatically.

```kotlin
router("smart-assistant") {
    // Route expensive reasoning to Claude
    policy {
        condition { it.prompt.contains("analyze", ignoreCase = true) }
        use("claude-3-5-sonnet-20241022")
    }
    // Default to a cheap, fast model
    default("gpt-4o-mini")
}
```

### Benefits of Routing:
- **Cost Optimization**: Use small models for simple tasks.
- **Resilience**: Automatically switch providers if one is down.
- **Specialization**: Route Vision tasks only to Vision models.

## 4. Configuration DSL

Set up your entire model ecosystem in one block:

```kotlin
MobileGraph.initialize(context) {
    withModels {
        openai(apiKey = "...")
        gemini(apiKey = "...")
        claude(apiKey = "...")
        huggingface(apiKey = "...", name = "meta-llama/Llama-3.2-1B-Instruct")
        
        router("main") {
            default("gpt-4o-mini")
        }
    }
}
```

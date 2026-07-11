# Multi-Model Orchestration Guide

Phase 4 of MobileGraph expands the ecosystem to support multiple cloud-based LLM providers, multi-modal inputs (Vision), and unified error handling.

---

## 1. Supported Providers

MobileGraph now natively supports the major cloud AI providers:

### OpenAI
The default provider, supporting `gpt-4o`, `gpt-4o-mini`, and legacy models.
```kotlin
openai(apiKey = "sk-...") {
    isDefault = true
}
```

### Google Gemini
Native integration for the Android ecosystem. Supports `gemini-1.5-pro` and `gemini-1.5-flash`.
```kotlin
gemini(apiKey = "...") {
    defaultConfig { temperature = 0.7f }
}
```

### Anthropic Claude
Industry-leading reasoning models. Supports `claude-3-5-sonnet` and `claude-3-haiku`.
```kotlin
claude(apiKey = "...") {
    defaultConfig { maxTokens = 2048 }
}
```

### OpenRouter
A unified gateway to access over 100+ models (including Llama 3, DeepSeek, etc.) via a single API key.
```kotlin
openrouter(apiKey = "...", name = "meta-llama/llama-3.1-70b-instruct")
```

### Groq
Ultra-fast inference for open-source models like Llama 3.
```kotlin
groq(apiKey = "...") {
    chat(name = "llama-3.1-70b-versatile")
}
```

### DeepSeek
Powerful and cost-effective models.
```kotlin
deepseek(apiKey = "...") {
    chat(name = "deepseek-chat")
}
```

---

## 2. Multi-Modal (Vision)

You can now send images and files to models that support Vision (e.g., GPT-4o, Claude 3.5).

```kotlin
val message = UserMessage(
    parts = listOf(
        ContentPart.Text("What is in this image?"),
        ContentPart.Image(data = "https://example.com/photo.jpg", mediaType = "image/jpeg")
    )
)
```

MobileGraph standardizes these parts across all providers, translating them into the specific format required by OpenAI, Gemini, or Claude.

---

## 3. Unified Error Handling

Different providers return errors in different formats. MobileGraph translates these into a unified exception hierarchy:

- `AuthenticationException`: Invalid API keys or unauthorized access.
- `RateLimitException`: Too many requests.
- `InvalidModelException`: Model name is wrong or not accessible.
- `SafetyException`: Content was blocked by provider filters.
- `ContextLengthException`: The prompt is too long for the model.

This allows you to write resilient error handling once:

```kotlin
try {
    val result = model.invoke(prompt)
} catch (e: RateLimitException) {
    // Show "Try again in a minute" to user
} catch (e: SafetyException) {
    // Show content warning
}
```

---

## 4. Why use Multi-Model Support?

1.  **Redundancy**: If one provider goes down, switch to another with a single line of code.
2.  **Cost Optimization**: Use cheap models (`gpt-4o-mini`) for simple tasks and powerful models (`claude-3-5-sonnet`) for complex reasoning.
3.  **Specialization**: Use Gemini for its massive context window and Claude for its coding/reasoning capabilities.

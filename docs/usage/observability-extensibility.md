# Observability & Extensibility Guide

This guide covers how to monitor, log, and extend the MobileGraph ADK.

---

## 1. Middleware (Cross-Cutting Concerns)

**What**: A "hook" system that intercepts requests and responses to add behaviors like logging, retries, or security.

**Why**: Decouples "business logic" (AI) from "infrastructure logic" (logging). It allows you to add features like automatic retries to *any* model without modifying its source code.

**How**:
```kotlin
class SecurityMiddleware : ChatModelMiddleware {
    override suspend fun intercept(input: ChatModelInput, context: ExecutionContext, next: ...) : ModelOutput {
        val redacted = redactPII(input.prompt)
        return next(input.copy(prompt = redacted), context)
    }
}
```

---

## 2. Passive Observability (Events)

**What**: A real-time stream of events representing the internal lifecycle of AI operations (requests starting, completing, or failing).

**Why**: To build real-time UI indicators (loading states) and track metrics without manual logging at every call site.

**How**:
```kotlin
viewModelScope.launch {
    session.events.collect { event ->
        when (event) {
            is MobileGraphEvent.RequestStarted -> println("AI is thinking...")
            is MobileGraphEvent.RequestCompleted -> println("AI finished!")
            is MobileGraphEvent.RequestFailed -> println("AI error: ${event.errorMessage}")
        }
    }
}
```

---

## 3. Custom Logging (Android Logcat)

**What**: Integration of platform-specific logging into the ADK via the `MobileGraphLogger` interface.

**Why**: Integrate with native logging systems (like Android's Logcat) for better filtering and debugging.

**How**:
```kotlin
class ApplicationLogger : MobileGraphLogger {
    override fun log(message: String, severity: MobileGraphLogger.Severity) {
        when(severity) {
            MobileGraphLogger.Severity.ERROR -> Log.e("MobileGraph", message)
            // ... handle other severities
        }
    }
}

// Register via LoggingMiddleware in Initialization
withModels {
    chat("gpt-4o", model) {
        middleware {
            +LoggingMiddleware(logger = ApplicationLogger())
        }
    }
}
```

---

## 4. Mobile Lifecycle & Performance (KPIs)

### Memory Management
AI history can consume significant RAM. Always close sessions when they are no longer needed.
```kotlin
class ChatViewModel(val session: MobileGraphSession) : ViewModel() {
    override fun onCleared() {
        session.close() // Triggers internal cleanup
    }
}
```

### Network Resilience
Always use the `RetryMiddleware` with exponential backoff to handle "dead zones" without draining the battery.
```kotlin
middleware {
    +RetryMiddleware(maxRetries = 3)
}
```

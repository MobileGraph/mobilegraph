# Memory & State Guide

This guide covers how MobileGraph manages conversation history and ensures resilience across mobile lifecycle events.

---

## 1. Chat Memory & Persistence

**What**: A system that automatically manages conversation history and its persistence to local storage.

**Why**: Mobile apps have sporadic connectivity and frequent process deaths. Chat memory ensures the AI remembers context and the user never loses their data.

**How**:
```kotlin
// Implement custom storage (e.g., SQLDelight or Room)
class MyDbHistory : ChatMessageHistory {
    override suspend fun add(item: ChatMessage, context: ExecutionContext) { /* Save to DB */ }
    override suspend fun get(context: ExecutionContext): List<ChatMessage> { /* Fetch */ }
}

// Register in initialization
withMemory {
    useChatMemory(MyDbHistory())
}
```

---

## 2. Mobile-First Memory Strategies

**What**: Heuristics to manage how much history is sent to the LLM (Sliding Window vs. Summarization).

**Why**: Sending 100 previous messages to an LLM is expensive and drains battery. Strategies keep the context relevant while staying within budget.

**How**:
```kotlin
// Option A: Sliding Window (Keep last 5 turns)
withMemory { useWindowChatMemory(k = 5) }

// Option B: Summarization (Summarize old messages)
withMemory { useSummaryBufferMemory(maxBufferMessages = 10, summarizeModel = model) }
```

---

## 3. Local-First Sync & Idempotency

**What**: Every `ChatMessage` includes a UUID and a `SyncState` (PENDING, SYNCED, FAILED).

**Why**: To handle "dead zones." If a network call fails, the message is saved locally with a `PENDING` flag. The UUID ensures that if a retry is sent, the server doesn't process the same message twice.

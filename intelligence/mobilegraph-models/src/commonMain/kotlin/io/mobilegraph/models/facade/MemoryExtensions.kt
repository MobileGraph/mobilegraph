package io.mobilegraph.models.facade

import io.mobilegraph.core.configuration.MemoryConfiguration
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.memory.ChatMemory
import io.mobilegraph.models.memory.ChatMessageHistory
import io.mobilegraph.models.memory.ConversationBufferWindowMemory
import io.mobilegraph.models.memory.ConversationSummaryBufferMemory
import io.mobilegraph.models.memory.InMemoryChatMessageHistory

/**
 * Entry point for configuring memory in the MobileGraph environment.
 * Using this block ensures that only one primary memory strategy is active.
 */
fun MobileGraphEnvironment.Builder.withMemory(block: MemoryConfiguration.() -> Unit) =
    apply {
        val config = MemoryConfiguration()
        config.block()
        config.history?.let {
            val history = it as ChatMessageHistory
            component(ChatMessageHistory::class, history)
            // Also register under base interface for core cleanup logic
            component(ChatMemory::class, history)
        }
    }

/**
 * Configures basic chat memory.
 */
fun MemoryConfiguration.useChatMemory(history: ChatMessageHistory = InMemoryChatMessageHistory()) {
    this.history = history
}

/**
 * Configures sliding window chat memory based on interaction turns.
 */
fun MemoryConfiguration.useWindowChatMemory(k: Int = 5) {
    this.history = ConversationBufferWindowMemory(k)
}

/**
 * Configures summary buffer memory that keeps recent messages raw and summarizes older ones.
 */
fun MemoryConfiguration.useSummaryBufferMemory(
    maxBufferMessages: Int = 10,
    modelName: String,
) {
    this.history = ConversationSummaryBufferMemory(maxBufferMessages, modelName)
}

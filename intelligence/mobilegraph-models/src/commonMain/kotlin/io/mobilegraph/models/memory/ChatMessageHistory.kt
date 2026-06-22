/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.memory

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.memory.ChatMemory
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatMessage
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SystemMessage
import io.mobilegraph.models.facade.models

/**
 * Specialized memory for [ChatMessage] history.
 *
 * Implementations of this interface handle the storage and retrieval of conversation
 * turns between the human and the assistant, keyed by session.
 */
interface ChatMessageHistory : ChatMemory<ChatMessage>

/**
 * Internal helper to resolve the session ID from the context.
 */
private fun getSessionId(context: ExecutionContext): String = context.sessionId?.value ?: "global"

/**
 * Basic in-memory implementation of [ChatMessageHistory].
 *
 * This implementation groups messages by [sessionId] and does not survive process death.
 */
class InMemoryChatMessageHistory : ChatMessageHistory {
    private val sessions = mutableMapOf<String, MutableList<ChatMessage>>()

    override suspend fun add(
        item: ChatMessage,
        context: ExecutionContext,
    ) {
        val sessionId = getSessionId(context)
        println("Memory: Adding message for session [$sessionId] - ${item.content.take(20)}...")
        val messages = sessions.getOrPut(sessionId) { mutableListOf() }

        // Update if exists, otherwise append
        val index = messages.indexOfFirst { it.id == item.id && it.id.isNotEmpty() }
        if (index != -1) {
            messages[index] = item
        } else {
            messages.add(item)
        }
    }

    override suspend fun get(context: ExecutionContext): List<ChatMessage> {
        val sessionId = getSessionId(context)
        val messages = sessions[sessionId] ?: emptyList()
        println("Memory: Retrieving ${messages.size} messages for session [$sessionId]")
        return messages.toList()
    }

    override suspend fun clear(context: ExecutionContext) {
        sessions.remove(getSessionId(context))
    }

    override suspend fun clearAll(context: ExecutionContext) {
        sessions.clear()
    }
}

/**
 * Keeps only the last [k] interactions (turns) per session.
 * One interaction = One HumanMessage + One AssistantMessage.
 *
 * @param k The number of complete interactions to keep per session.
 */
class ConversationBufferWindowMemory(
    private val k: Int,
) : ChatMessageHistory {
    private val sessions = mutableMapOf<String, MutableList<ChatMessage>>()

    override suspend fun add(
        item: ChatMessage,
        context: ExecutionContext,
    ) {
        val sessionId = getSessionId(context)
        val messages = sessions.getOrPut(sessionId) { mutableListOf() }
        messages.add(item)

        val assistantMessages = messages.filterIsInstance<AssistantMessage>()
        if (assistantMessages.size > k) {
            val firstAssistantIndex = messages.indexOfFirst { it is AssistantMessage }
            if (firstAssistantIndex != -1) {
                repeat(firstAssistantIndex + 1) {
                    if (messages.isNotEmpty()) messages.removeAt(0)
                }
            }
        }
    }

    override suspend fun get(context: ExecutionContext): List<ChatMessage> = sessions[getSessionId(context)]?.toList() ?: emptyList()

    override suspend fun clear(context: ExecutionContext) {
        sessions.remove(getSessionId(context))
    }

    override suspend fun clearAll(context: ExecutionContext) {
        sessions.clear()
    }
}

/**
 * Keeps recent messages raw, and summarizes older ones into a rolling summary per session.
 *
 * @param maxBufferMessages The maximum number of raw messages to keep before triggering summarization.
 * @param summarizeModel The [ChatModel] used to generate the summary text.
 */
class ConversationSummaryBufferMemory(
    private val maxBufferMessages: Int,
    private val summarizeModel: String? = null,
) : ChatMessageHistory {
    private data class SessionData(
        var summary: String? = null,
        val buffer: MutableList<ChatMessage> = mutableListOf(),
    )

    private val sessions = mutableMapOf<String, SessionData>()

    override suspend fun add(
        item: ChatMessage,
        context: ExecutionContext,
    ) {
        val sessionId = getSessionId(context)
        val data = sessions.getOrPut(sessionId) { SessionData() }
        data.buffer.add(item)

        if (data.buffer.size > maxBufferMessages && summarizeModel != null) {
            summarize(sessionId, context)
        }
    }

    private suspend fun summarize(
        sessionId: String,
        context: ExecutionContext,
    ) {
        val data = sessions[sessionId] ?: return
        val modelName = summarizeModel ?: return

        val registry = MobileGraph.models.registry()
        val model = registry.chat(modelName) ?: return

        val toSummarize = data.buffer.take(data.buffer.size - (maxBufferMessages / 2))
        repeat(toSummarize.size) { data.buffer.removeAt(0) }

        val prompt =
            "Summarize the following conversation history concisely, incorporating it into the existing summary: \n" +
                "Existing Summary: ${data.summary ?: "None"}\n" +
                "New Messages: ${toSummarize.joinToString("\n") { "${it.toMessage().role}: ${it.content}" }}"

        val output = model.invoke(ChatPromptValue(listOf(SystemMessage(prompt))), context = context)
        if (output is ModelOutput.ChatOutput) {
            data.summary = output.message.content
        }
    }

    override suspend fun get(context: ExecutionContext): List<ChatMessage> {
        val data = sessions[getSessionId(context)] ?: return emptyList()
        val result = mutableListOf<ChatMessage>()
        data.summary?.let {
            result.add(SystemMessage("Previous conversation summary: $it", sessionId = getSessionId(context)))
        }
        result.addAll(data.buffer)
        return result
    }

    override suspend fun clear(context: ExecutionContext) {
        sessions.remove(getSessionId(context))
    }

    override suspend fun clearAll(context: ExecutionContext) {
        sessions.clear()
    }
}

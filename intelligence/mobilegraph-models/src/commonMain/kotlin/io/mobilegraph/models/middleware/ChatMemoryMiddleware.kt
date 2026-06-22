/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.registry.getComponent
import io.mobilegraph.models.AssistantMessage
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.SyncState
import io.mobilegraph.models.memory.ChatMessageHistory

/**
 * Middleware that automatically manages chat history, organized by session.
 *
 * This middleware handles:
 * 1. Loading previous history for the current session and prepending it to the prompt.
 * 2. Saving new human messages to memory before the model call (Local-First).
 * 3. Saving the assistant's response to memory after a successful call.
 * 4. Tagging all messages with the active [sessionId] from the context.
 */
class ChatMemoryMiddleware : ChatModelMiddleware {
    override suspend fun intercept(
        input: ChatModelInput,
        context: ExecutionContext,
        next: suspend (ChatModelInput, ExecutionContext) -> ModelOutput,
    ): ModelOutput {
        // Resolve memory implementation from context or global environment
        val memory =
            context.getComponent<ChatMessageHistory>()
                ?: try {
                    MobileGraph.instance.environment.getComponent(ChatMessageHistory::class)
                } catch (e: Exception) {
                    null
                }
                ?: return next(input, context)

        // Resolve active session ID
        val activeSessionId = context.sessionId?.value ?: "global"

        // 1. Load history for this session and append it to the current prompt
        val history = memory.get(context)
        val combinedMessages = history + input.prompt.messages
        val enrichedInput =
            input.copy(
                prompt = ChatPromptValue(combinedMessages),
            )

        // 2. Add current human messages to memory (Tag with sessionId and mark as PENDING)
        input.prompt.messages.forEach { msg ->
            val sessionTaggedMsg =
                msg.copyWith(
                    sessionId = activeSessionId,
                    syncState = SyncState.PENDING,
                )
            memory.add(sessionTaggedMsg, context)
        }

        // 3. Execute the rest of the chain (Idempotent: Uses original IDs)
        val output = next(enrichedInput, context)

        // 4. On successful response, update memory
        if (output is ModelOutput.ChatOutput) {
            // Add assistant response (Tagged with sessionId and marked as SYNCED)
            val assistantMsg =
                output.message.copyWith(
                    sessionId = activeSessionId,
                    syncState = SyncState.SYNCED,
                ) as AssistantMessage

            memory.add(assistantMsg, context)

            // Promote human messages to SYNCED
            input.prompt.messages.forEach { msg ->
                memory.add(msg.copyWith(sessionId = activeSessionId, syncState = SyncState.SYNCED), context)
            }

            return output.copy(message = assistantMsg)
        }

        return output
    }
}

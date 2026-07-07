/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.facade

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.runtime.publishEvent
import io.mobilegraph.core.session.MobileGraphSession
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.UserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * Retrieves the [ChatModel] bound to this session.
 *
 * **Usage Recommendation**: Use this for **Stateful Conversational UI**.
 * The returned model is automatically tied to this session's ID and history.
 *
 * If a specific model name was provided during session creation, that model is returned.
 * Otherwise, the SDK's default chat model is used.
 *
 * @return The [ChatModel] to be used for this session.
 * @throws IllegalStateException if no suitable model can be found in the registry.
 */
fun MobileGraphSession.model(): ChatModel {
    val registry =
        MobileGraphModels(
            environment.getComponent(io.mobilegraph.core.facade.MobileGraph::class)
                ?: io.mobilegraph.core.facade.MobileGraph.instance,
        ).registry()

    return if (modelName != null) {
        registry.chat(modelName!!) ?: throw IllegalStateException("Model '$modelName' bound to session not found")
    } else {
        registry.chat() ?: throw IllegalStateException("No default ChatModel registered")
    }
}

/**
 * Extension function to invoke a chat model through a MobileGraph session.
 *
 * This is a high-level convenience method for simple Q&A. For advanced usage,
 * use [model] to get the session-bound model directly.
 *
 * @param query The user's input text.
 * @param config Optional model configuration.
 * @param context Optional execution context.
 * @return The [ModelOutput] from the model.
 */
suspend fun MobileGraphSession.chat(
    query: String,
    config: ModelConfig? = null,
    context: ExecutionContext? = null,
): ModelOutput {
    val chatModel = model()
    val prompt = ChatPromptValue(messages = listOf(UserMessage(query)))

    val traceId = context?.traceId ?: TraceId("trace-$sessionId")
    val requestId = context?.requestId ?: RequestId("req-${kotlin.random.Random.nextInt()}")

    val effectiveContext =
        context ?: SimpleExecutionContext(
            traceId = traceId,
            requestId = requestId,
            sessionId = SessionId(sessionId),
            componentProvider = internal,
        )

    publishEvent(MobileGraphEvent.RequestStarted(traceId, requestId, SessionId(sessionId)))

    return try {
        val response = chatModel.invoke(prompt = prompt, config = config, context = effectiveContext)
        publishEvent(MobileGraphEvent.RequestCompleted(traceId, requestId, SessionId(sessionId)))
        response
    } catch (e: Exception) {
        publishEvent(MobileGraphEvent.RequestFailed(traceId, requestId, e.message ?: "Unknown error", SessionId(sessionId)))
        ModelOutput.ErrorOutput(e)
    }
}

/**
 * Extension function to stream from a chat model through a MobileGraph session.
 *
 * @param query The user's input text.
 * @param config Optional model configuration.
 * @param context Optional execution context.
 * @return A [Flow] of [ChatChunk] from the model.
 */
fun MobileGraphSession.stream(
    query: String,
    config: ModelConfig? = null,
    context: ExecutionContext? = null,
): Flow<ChatChunk> {
    val chatModel = model()
    val prompt = ChatPromptValue(messages = listOf(UserMessage(query)))

    val traceId = context?.traceId ?: TraceId("trace-$sessionId")
    val requestId = context?.requestId ?: RequestId("req-${kotlin.random.Random.nextInt()}")

    val effectiveContext =
        context ?: SimpleExecutionContext(
            traceId = traceId,
            requestId = requestId,
            sessionId = SessionId(sessionId),
            componentProvider = internal,
        )

    return chatModel
        .stream(prompt = prompt, config = config, context = effectiveContext)
        .onStart {
            publishEvent(MobileGraphEvent.RequestStarted(traceId, requestId, SessionId(sessionId)))
        }.onCompletion { cause ->
            if (cause == null) {
                publishEvent(MobileGraphEvent.RequestCompleted(traceId, requestId, SessionId(sessionId)))
            } else {
                publishEvent(
                    MobileGraphEvent.RequestFailed(
                        traceId,
                        requestId,
                        cause.message ?: "Stream cancelled or failed",
                        SessionId(sessionId),
                    ),
                )
            }
        }
}

/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.tools
import kotlinx.coroutines.flow.Flow

/**
 * Interface for chat-based Large Language Models (LLMs).
 *
 * This is the primary interface for interacting with conversational AI providers
 * like OpenAI, Gemini, or custom private servers.
 */
interface ChatModel : Model {
    /**
     * Invokes the model with a prompt and returns a complete output.
     *
     * @param prompt The prompt value containing the message history.
     * @param config Optional model-specific configuration (temperature, max tokens, etc.).
     * @param context The execution context for the request.
     * @return The [ModelOutput] produced by the model.
     */
    suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig? = null,
        context: ExecutionContext = ExecutionContext.Empty,
    ): ModelOutput

    /**
     * Streams the model response as a flow of chunks.
     *
     * @param prompt The prompt value containing the message history.
     * @param config Optional model-specific configuration.
     * @param context The execution context for the request.
     * @return A [Flow] of [ChatChunk] objects.
     */
    fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig? = null,
        context: ExecutionContext = ExecutionContext.Empty,
    ): Flow<ChatChunk>

    /**
     * Internal utility to find and execute a registered tool.
     *
     * This method is typically used by model implementations to handle function calls.
     *
     * @param toolName The name of the tool to execute.
     * @param toolArgs The input arguments for the tool.
     * @param context The execution context.
     * @return A string representation of the tool's output.
     */
    suspend fun fetchAndExecuteTool(
        toolName: String,
        toolArgs: Any,
        context: ExecutionContext = ExecutionContext.Empty,
    ): String {
        val tool = MobileGraph.tools.registry().get(toolName)
        return if (tool != null) {
            try {
                @Suppress("UNCHECKED_CAST")
                val toolInstance = tool as Tool<Any, Any>

                val output = toolInstance.invoke(toolArgs, context)
                output.toString()
            } catch (e: Exception) {
                "Error executing tool $toolName: ${e.message}"
            }
        } else {
            "Tool $toolName not found"
        }
    }

    fun readModelConfig(): ModelConfig?
}

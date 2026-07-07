/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.facade

import io.mobilegraph.core.configuration.ModelsConfiguration
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.tools.ToolDefinition
import io.mobilegraph.models.ChatChunk
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.EmbeddingModel
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.ModelRegistry
import io.mobilegraph.models.middleware.ChatModelMiddleware
import io.mobilegraph.models.middleware.MiddlewareChatModel
import io.mobilegraph.models.registry.DefaultModelRegistry
import kotlinx.coroutines.flow.Flow

/**
 * Extension for the models { } DSL block.
 */

private fun ModelsConfiguration.getOrCreateRegistry(): DefaultModelRegistry {
    // This is a bit of a hack because ModelsConfiguration is a placeholder.
    // In a real implementation, we would store this in a field.
    // For now, we'll use a registry and attach it to the environment.
    return registry as? DefaultModelRegistry ?: DefaultModelRegistry().also { registry = it }
}

private var ModelsConfiguration.registry: ModelRegistry? by kotlin.properties.Delegates.observable(null) { _, _, _ -> }

/**
 * Builder for configuring a [ChatModel] within the DSL.
 *
 * @property name The unique name for this model in the registry.
 * @property model The base [ChatModel] implementation.
 */
class ChatModelBuilder(
    val name: String,
    val model: ChatModel,
) {
    /**
     * Whether this model should be the default for chat operations.
     */
    var isDefault: Boolean = false

    private val middlewares = mutableListOf<ChatModelMiddleware>()
    private var defaultConfig: ModelConfig? = null

    /**
     * Configures default settings for this model.
     *
     * @param block DSL block to configure [ModelConfig].
     */
    fun defaultConfig(block: ModelConfigBuilder.() -> Unit) {
        val builder = ModelConfigBuilder()
        builder.block()
        defaultConfig = builder.build()
    }

    /**
     * Attaches middlewares to this model. Middlewares can act as an interceptor. A "hook" system that intercepts requests and responses to add behaviors like logging, retries, or security.
     * It helps to Decouple "business logic" (AI) from "infrastructure logic" (logging). It allows you to add features like automatic retries to any model without modifying its source code.
     *
     * @param block DSL block to add [ChatModelMiddleware]s.
     */
    fun middleware(block: MiddlewareBuilder.() -> Unit) {
        val builder = MiddlewareBuilder()
        builder.block()
        middlewares.addAll(builder.middlewares)
    }

    /**
     * Builds the final [ChatModel] instance, wrapping it with configuration and middlewares.
     */
    fun build(): ChatModel {
        val baseModel =
            if (defaultConfig != null) {
                ConfiguredChatModel(model, defaultConfig!!)
            } else {
                model
            }

        return if (middlewares.isNotEmpty()) {
            MiddlewareChatModel(baseModel, middlewares)
        } else {
            baseModel
        }
    }
}

/**
 * Builder for [ModelConfig] within the DSL.
 */
class ModelConfigBuilder {
    /**
     * The temperature for sampling (0.0 to 1.0).
     */
    var temperature: Float? = null

    /**
     * Maximum number of tokens to generate.
     */
    var maxTokens: Int? = null

    /**
     * List of sequences where the model should stop generating.
     */
    var stop: List<String>? = null

    var tools: List<ToolDefinition>? = null

    /**
     * Builds the [ModelConfig] instance.
     */
    fun build() = ModelConfig(temperature, maxTokens, stop, tools)
}

/**
 * Builder for collecting middlewares.
 */
class MiddlewareBuilder {
    internal val middlewares = mutableListOf<ChatModelMiddleware>()

    /**
     * Adds a middleware to the list using the unary plus operator.
     */
    operator fun ChatModelMiddleware.unaryPlus() {
        middlewares.add(this)
    }
}

/**
 * A wrapper that applies default configuration to every model invocation.
 */
private class ConfiguredChatModel(
    private val delegate: ChatModel,
    private val defaultConfig: ModelConfig,
) : ChatModel by delegate {
    override suspend fun invoke(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: io.mobilegraph.core.context.ExecutionContext,
    ): ModelOutput {
        val mergedConfig =
            config?.copy(
                temperature = config.temperature ?: defaultConfig.temperature,
                maxTokens = config.maxTokens ?: defaultConfig.maxTokens,
                stop = config.stop ?: defaultConfig.stop,
                tools = config.tools,
            )
                ?: defaultConfig
        return delegate.invoke(prompt, mergedConfig, context)
    }

    override fun stream(
        prompt: ChatPromptValue,
        config: ModelConfig?,
        context: io.mobilegraph.core.context.ExecutionContext,
    ): Flow<ChatChunk> {
        val mergedConfig =
            config?.copy(
                temperature = config.temperature ?: defaultConfig.temperature,
                maxTokens = config.maxTokens ?: defaultConfig.maxTokens,
                stop = config.stop ?: defaultConfig.stop,
            )
                ?: defaultConfig
        return delegate.stream(prompt, mergedConfig, context)
    }

    override fun readModelConfig(): ModelConfig? = defaultConfig
}

/**
 * Registers a [ChatModel] in the configuration.
 *
 * @param name The name to register the model under.
 * @param model The model implementation.
 * @param block Optional builder block for additional configuration.
 */
fun ModelsConfiguration.chat(
    name: String,
    model: ChatModel,
    block: ChatModelBuilder.() -> Unit = {},
) {
    val builder = ChatModelBuilder(name, model)
    builder.block()
    getOrCreateRegistry().registerChat(name, builder.build(), builder.isDefault)
}

/**
 * Registers a [ChatModel] in the configuration using its intrinsic name.
 */
fun ModelsConfiguration.chat(
    model: ChatModel,
    block: ChatModelBuilder.() -> Unit = {},
) {
    chat(model.name, model, block)
}

/**
 * Registers an [EmbeddingModel] in the configuration.
 *
 * @param name The name to register the model under.
 * @param model The embedding model implementation.
 * @param isDefault Whether this should be the default embedding model.
 */
fun ModelsConfiguration.embedding(
    name: String,
    model: EmbeddingModel,
    isDefault: Boolean = false,
) {
    getOrCreateRegistry().registerEmbedding(name, model, isDefault)
}

/**
 * Registers an [EmbeddingModel] in the configuration using its intrinsic name.
 */
fun ModelsConfiguration.embedding(
    model: EmbeddingModel,
    isDefault: Boolean = false,
) {
    embedding(model.name, model, isDefault)
}

/**
 * Attaches the model registry to the MobileGraph environment.
 *
 * @param block DSL block for model registration.
 */
fun MobileGraphEnvironment.Builder.withModels(block: ModelsConfiguration.() -> Unit) =
    apply {
        val config = ModelsConfiguration()
        config.block()
        config.registry?.let {
            component(ModelRegistry::class, it)
        }
    }

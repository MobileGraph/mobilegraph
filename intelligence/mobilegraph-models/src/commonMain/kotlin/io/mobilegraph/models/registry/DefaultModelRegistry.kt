package io.mobilegraph.models.registry

import io.mobilegraph.core.capability.Capability
import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.EmbeddingModel
import io.mobilegraph.models.ModelRegistry

/**
 * Default implementation of [ModelRegistry].
 */
internal class DefaultModelRegistry : ModelRegistry {
    private val chatModels = mutableMapOf<String, ChatModel>()
    private val embeddingModels = mutableMapOf<String, EmbeddingModel>()

    private var defaultChatModel: String? = null
    private var defaultEmbeddingModel: String? = null

    fun registerChat(
        name: String,
        model: ChatModel,
        isDefault: Boolean = false,
    ) {
        chatModels[name] = model
        if (isDefault || defaultChatModel == null) {
            defaultChatModel = name
        }
    }

    fun registerEmbedding(
        name: String,
        model: EmbeddingModel,
        isDefault: Boolean = false,
    ) {
        embeddingModels[name] = model
        if (isDefault || defaultEmbeddingModel == null) {
            defaultEmbeddingModel = name
        }
    }

    override fun chat(name: String): ChatModel? = chatModels[name]

    override fun chat(): ChatModel? = defaultChatModel?.let { chatModels[it] }

    override fun chatFor(capability: Capability): ChatModel? = chatModels.values.find { it.supports(capability) }

    override fun embedding(name: String): EmbeddingModel? = embeddingModels[name]

    override fun embedding(): EmbeddingModel? = defaultEmbeddingModel?.let { embeddingModels[it] }
}

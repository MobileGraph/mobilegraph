package io.mobilegraph.tools.selection

import io.mobilegraph.core.tools.EmbeddingStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A simple persistent embedding store that saves to a JSON string.
 * This can be used with a local file or SharedPreferences.
 */
class JsonEmbeddingStore(
    private val initialData: String? = null,
    private val onSave: (String) -> Unit,
) : EmbeddingStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val storage: MutableMap<String, List<Float>> =
        initialData?.let {
            try {
                json.decodeFromString<Map<String, List<Float>>>(it).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
        } ?: mutableMapOf()

    override fun get(key: String): List<Float>? = storage[key]

    override fun put(
        key: String,
        embedding: List<Float>,
    ) {
        storage[key] = embedding
        onSave(json.encodeToString(storage))
    }

    override fun getAll(): Map<String, List<Float>> = storage.toMap()
}

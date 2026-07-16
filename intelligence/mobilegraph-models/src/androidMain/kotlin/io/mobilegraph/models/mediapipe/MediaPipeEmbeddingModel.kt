package io.mobilegraph.models.mediapipe

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.EmbeddingModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An offline embedding model implementation using MediaPipe Tasks.
 *
 * MediaPipe's [TextEmbedder] wraps a TFLite interpreter whose internal state is
 * **not thread-safe**. All calls to [embed] are serialized through a [Mutex] to
 * prevent concurrent native access, which would corrupt memory and cause a
 * SIGSEGV (SEGV_MAPERR).
 *
 * @param contextProvider A function that provides the Android Context.
 * @param modelPath Path to the TFLite model file in the assets folder. Download from: https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite
 */
class MediaPipeEmbeddingModel(
    private val contextProvider: () -> Any,
    private val modelPath: String = "universal_sentence_encoder.tflite",
) : EmbeddingModel {
    override val name: String = "mediapipe-universal-sentence-encoder"

    /** Guards all access to [textEmbedder] — both initialization and inference. */
    private val mutex = Mutex()

    @Volatile
    private var textEmbedder: TextEmbedder? = null

    @Volatile
    private var closed = false

    private fun getOrCreateTextEmbedder(): TextEmbedder {
        // Fast path – already initialized (volatile read is safe).
        textEmbedder?.let { return it }

        // Slow path – double-checked locking via synchronized for one-time init.
        return synchronized(this) {
            textEmbedder ?: run {
                check(!closed) { "MediaPipeEmbeddingModel has been closed" }

                val context = contextProvider() as android.content.Context

                val baseOptions =
                    BaseOptions
                        .builder()
                        .setModelAssetPath(modelPath)
                        .build()

                val options =
                    TextEmbedderOptions
                        .builder()
                        .setBaseOptions(baseOptions)
                        .build()

                TextEmbedder.createFromOptions(context, options).also {
                    textEmbedder = it
                }
            }
        }
    }

    override fun supports(capability: Capability): Boolean = false

    override suspend fun embed(
        text: String,
        context: ExecutionContext,
    ): FloatArray =
        // The Mutex ensures only one coroutine enters the native TextEmbedder at
        // a time, regardless of which dispatcher thread it resides on.
        mutex.withLock {
            check(!closed) { "MediaPipeEmbeddingModel has been closed" }
            val result = getOrCreateTextEmbedder().embed(text)
            result
                .embeddingResult()
                .embeddings()
                .first()
                .floatEmbedding()!!
        }

    override suspend fun embed(
        texts: List<String>,
        context: ExecutionContext,
    ): List<FloatArray> = texts.map { embed(it, context) }

    /**
     * Releases the native TFLite resources held by the underlying [TextEmbedder].
     * After calling this, any subsequent [embed] call will throw [IllegalStateException].
     */
    fun close() {
        synchronized(this) {
            closed = true
            textEmbedder?.close()
            textEmbedder = null
        }
    }
}

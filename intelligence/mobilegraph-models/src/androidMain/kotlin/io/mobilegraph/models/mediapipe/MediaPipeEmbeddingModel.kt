package io.mobilegraph.models.mediapipe

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import io.mobilegraph.core.capability.Capability
import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.EmbeddingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * An offline embedding model implementation using MediaPipe Tasks.
 *
 * @param contextProvider A function that provides the Android Context.
 * @param modelPath Path to the TFLite model file in the assets folder. Download from: https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite
 */
class MediaPipeEmbeddingModel(
    private val contextProvider: () -> Any,
    private val modelPath: String = "universal_sentence_encoder.tflite",
) : EmbeddingModel {
    override val name: String = "mediapipe-universal-sentence-encoder"

    private var textEmbedder: TextEmbedder? = null

    private fun getTextEmbedder(): TextEmbedder =
        textEmbedder ?: synchronized(this) {
            textEmbedder ?: run {
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

                TextEmbedder.createFromOptions(context, options).also { textEmbedder = it }
            }
        }

    override fun supports(capability: Capability): Boolean = false

    override suspend fun embed(
        text: String,
        context: ExecutionContext,
    ): FloatArray =
        withContext(Dispatchers.Default) {
            val result = getTextEmbedder().embed(text)
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
}

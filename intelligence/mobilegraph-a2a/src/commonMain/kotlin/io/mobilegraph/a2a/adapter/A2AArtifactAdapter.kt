package io.mobilegraph.a2a.adapter

import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.DataPart
import io.mobilegraph.a2a.models.FilePart
import io.mobilegraph.a2a.models.TextPart

/**
 * Adapts A2A artifacts into MobileGraph-consumable data structures.
 *
 * Artifacts are the output/deliverables of an A2A task. This adapter
 * extracts and normalizes artifact content so that downstream
 * MobileGraph graph nodes can consume the data directly.
 *
 * ## Architecture
 * ```
 * Remote A2A Agent
 *        │
 *        │ Artifact
 *        v
 * A2AArtifactAdapter
 *        │
 *        v
 * MobileGraph Node (GraphState variables)
 *        │
 *        v
 * Next Graph Node
 * ```
 */
object A2AArtifactAdapter {
    /**
     * Extracts all text content from an artifact.
     *
     * @param artifact The A2A artifact.
     * @return Combined text from all [TextPart] parts.
     */
    fun extractText(artifact: A2AArtifact): String =
        artifact.parts
            .filterIsInstance<TextPart>()
            .joinToString("\n") { it.text }

    /**
     * Extracts all text content from a list of artifacts.
     *
     * @param artifacts The list of A2A artifacts.
     * @return Combined text from all artifacts' [TextPart] parts.
     */
    fun extractAllText(artifacts: List<A2AArtifact>): String = artifacts.joinToString("\n\n") { extractText(it) }

    /**
     * Extracts file references from an artifact.
     *
     * @param artifact The A2A artifact.
     * @return List of file data from [FilePart] parts.
     */
    fun extractFiles(artifact: A2AArtifact): List<ArtifactFile> =
        artifact.parts
            .filterIsInstance<FilePart>()
            .map { filePart ->
                ArtifactFile(
                    name = filePart.file.name,
                    mimeType = filePart.file.mimeType,
                    bytes = filePart.file.bytes,
                    uri = filePart.file.uri,
                )
            }

    /**
     * Extracts structured data from an artifact.
     *
     * @param artifact The A2A artifact.
     * @return List of structured data elements from [DataPart] parts.
     */
    fun extractData(artifact: A2AArtifact): List<String> =
        artifact.parts
            .filterIsInstance<DataPart>()
            .map { it.data.toString() }

    /**
     * Converts an artifact into a flat map suitable for graph state storage.
     *
     * @param artifact The A2A artifact.
     * @return A map of artifact properties.
     */
    fun toStateMap(artifact: A2AArtifact): Map<String, Any?> =
        buildMap {
            put("id", artifact.artifactId)
            put("name", artifact.name)
            put("description", artifact.description)
            put("text", extractText(artifact))
            put("files", extractFiles(artifact))
            put("data", extractData(artifact))
            put("index", artifact.index)
        }
}

/**
 * Represents a file extracted from an A2A artifact.
 */
data class ArtifactFile(
    /** File name. */
    val name: String?,
    /** MIME type (e.g., "application/pdf"). */
    val mimeType: String?,
    /** Base64-encoded file content (for inline files). */
    val bytes: String?,
    /** URI reference to the file (for remote files). */
    val uri: String?,
) {
    /** Whether this file has inline content. */
    val isInline: Boolean get() = bytes != null

    /** Whether this file is a remote reference. */
    val isRemote: Boolean get() = uri != null
}

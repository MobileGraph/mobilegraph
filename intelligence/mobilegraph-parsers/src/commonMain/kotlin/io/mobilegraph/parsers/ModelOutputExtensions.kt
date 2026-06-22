package io.mobilegraph.parsers

import io.mobilegraph.models.ModelOutput

/**
 * Extension to extract text from a ModelOutput.
 */
fun ModelOutput.asText(): String =
    when (this) {
        is ModelOutput.ChatOutput -> message.content
        is ModelOutput.ErrorOutput -> ""
    }

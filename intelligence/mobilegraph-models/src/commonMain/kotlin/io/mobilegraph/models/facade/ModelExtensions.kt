package io.mobilegraph.models.facade

import io.mobilegraph.models.ChatModel
import io.mobilegraph.models.ChatPromptValue
import io.mobilegraph.models.ModelOutput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

/**
 * Extension for batch processing multiple prompts.
 */
fun ChatModel.batch(prompts: List<ChatPromptValue>): Flow<ModelOutput> =
    prompts.asFlow().map { prompt ->
        this.invoke(prompt)
    }

package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.registry.getComponent
import io.mobilegraph.core.tools.AllToolSelector
import io.mobilegraph.core.tools.ToolSelector
import io.mobilegraph.core.tools.asDefinition
import io.mobilegraph.core.tools.toolRegistry
import io.mobilegraph.core.tools.tools
import io.mobilegraph.models.HumanMessage
import io.mobilegraph.models.ModelOutput

/**
 * Middleware that automatically selects relevant tools based on the user's query.
 */
class ToolSelectionMiddleware : ChatModelMiddleware {
    override suspend fun intercept(
        input: ChatModelInput,
        context: ExecutionContext,
        next: suspend (ChatModelInput, ExecutionContext) -> ModelOutput,
    ): ModelOutput {
        // If tools are already explicitly provided, don't override
        if (input.config?.tools != null) {
            return next(input, context)
        }

        val registry =
            context.toolRegistry ?: try {
                MobileGraph.tools.registry()
            } catch (e: Exception) {
                null
            }
                ?: return next(input, context)

        val selector =
            context.getComponent<ToolSelector>()
                ?: try {
                    MobileGraph.tools.selector()
                } catch (e: Exception) {
                    null
                }
                ?: AllToolSelector()

        // Extract the last human message as the query
        val query =
            input.prompt.messages
                .filterIsInstance<HumanMessage>()
                .lastOrNull()
                ?.content
                ?: return next(input, context)

        // Select relevant tools
        val selectedTools = selector.selectTools(query, registry, context)

        if (selectedTools.isEmpty()) {
            return next(input, context)
        }

        // Create a new config with the selected tools
        val newConfig =
            (input.config ?: io.mobilegraph.models.ModelConfig()).copy(
                tools = selectedTools.map { it.asDefinition() },
            )

        return next(input.copy(config = newConfig), context)
    }
}

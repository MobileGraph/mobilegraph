package io.mobilegraph.agents

import io.mobilegraph.core.annotations.InternalMobileGraphApi
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.registry.CompositeComponentProvider
import io.mobilegraph.core.registry.SimpleComponentProvider
import io.mobilegraph.core.tools.ToolRegistry
import io.mobilegraph.core.tools.asDefinition
import io.mobilegraph.core.tools.tools
import io.mobilegraph.graph.ExecutionResult
import io.mobilegraph.graph.GraphNode
import io.mobilegraph.models.ModelConfig
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.prompts.composition.promptComposer
import io.mobilegraph.state.GraphState
import io.mobilegraph.tools.registry.DefaultToolRegistry

/**
 * A specialized node that executes another [Agent].
 * This enables hierarchical and multi-agent orchestration.
 */
class AgentNode(
    override val id: String,
    private val agent: Agent,
    private val runtime: AgentRuntime,
) : GraphNode {
    @OptIn(InternalMobileGraphApi::class)
    override suspend fun execute(state: GraphState): ExecutionResult {
        if (agent.graph == null) {
            val userPrompt = agent.formatAgentInstruction(state)

            val systemMessage =
                if (agent.skills.isEmpty()) {
                    agent.rolePrompt
                } else {
                    val mergedContent = StringBuilder(agent.rolePrompt.content)
                    agent.skills.forEach { skill ->
                        mergedContent.append("\n\n")
                        mergedContent.append("=== ACTIVE SKILL: ${skill.name} ===\n")
                        skill.description?.let { mergedContent.append("Description: $it\n") }
                        mergedContent.append(skill.instructions)
                        mergedContent.append("\n====================================")
                    }
                    io.mobilegraph.models.SystemMessage(mergedContent.toString())
                }

            val prompt =
                promptComposer {
                    system(systemMessage)
                    user(userPrompt)
                }.compose()

            val chatModel = agent.model

            // Tool Resolution Logic
            val finalRegistry = resolveRegistry()

            val config =
                finalRegistry?.let { reg ->
                    var config = chatModel.readModelConfig()
                    if (config == null) {
                        config = ModelConfig()
                    }
                    val copiedConfig =
                        config.copy(
                            temperature = config.temperature,
                            maxTokens = config.maxTokens,
                            stop = config.stop,
                            tools = reg.getAll().map { it.asDefinition() },
                        )
                    copiedConfig
                }

            // Inject the resolved registry into the execution context
            val context =
                if (finalRegistry != null) {
                    val localProvider =
                        SimpleComponentProvider().apply {
                            register(ToolRegistry::class, finalRegistry)
                        }

                    val compositeProvider =
                        CompositeComponentProvider(
                            listOf(localProvider, state.executionContext),
                        )

                    (state.executionContext as? io.mobilegraph.core.context.SimpleExecutionContext)?.copy(
                        componentProvider = compositeProvider,
                    ) ?: state.executionContext
                } else {
                    state.executionContext
                }

            val output = agent.model.invoke(prompt, config, context)

            // Handle Network/LLM Errors explicitly
            if (output is ModelOutput.ErrorOutput) {
                return ExecutionResult.Error(state)
            }

            val newState = agent.handleLlmOutput(output, state)
            return ExecutionResult.Success(newState)
        } else {
            // Hierarchical execution: this node runs the sub-agent's graph
            return runtime.run(agent.graph!!, state)
        }
    }

    private fun resolveRegistry(): ToolRegistry? {
        val localTools = agent.tools
        val skillTools =
            if (agent.skills.isNotEmpty()) {
                DefaultToolRegistry().apply {
                    agent.skills.flatMap { it.tools }.forEach { register(it) }
                }
            } else {
                null
            }

        val globalTools =
            if (agent.useGlobalTools) {
                try {
                    MobileGraph.tools.registry()
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

        // Merge logic: Local > Skills > Global
        return when {
            (localTools != null || skillTools != null) && globalTools != null -> {
                DefaultToolRegistry().apply {
                    globalTools.getAll().forEach { register(it) }
                    skillTools?.getAll()?.forEach { register(it) }
                    localTools?.getAll()?.forEach { register(it) }
                }
            }

            localTools != null && skillTools != null -> {
                DefaultToolRegistry().apply {
                    skillTools.getAll().forEach { register(it) }
                    localTools.getAll().forEach { register(it) }
                }
            }

            localTools != null -> {
                localTools
            }

            skillTools != null -> {
                skillTools
            }

            globalTools != null -> {
                globalTools
            }

            else -> {
                null
            }
        }
    }
}

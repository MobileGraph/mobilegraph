package io.mobilegraph.models.middleware

import io.mobilegraph.core.context.ExecutionContext
import io.mobilegraph.models.ModelOutput

/**
 * Interface for custom logging implementations.
 */
interface MobileGraphLogger {
    fun log(
        message: String,
        severity: Severity = Severity.INFO,
    )

    enum class Severity { VERBOSE, DEBUG, INFO, WARN, ERROR }
}

/**
 * Default logger that prints to console/logcat.
 */
class DefaultMobileGraphLogger(
    private val tag: String = "MobileGraph",
) : MobileGraphLogger {
    override fun log(
        message: String,
        severity: MobileGraphLogger.Severity,
    ) {
        println("[$severity] $tag: $message")
    }
}

/**
 * Middleware that logs model inputs and outputs.
 */
class LoggingMiddleware(
    private val logger: MobileGraphLogger = DefaultMobileGraphLogger(),
    private val logPrompts: Boolean = true,
    private val logOutputs: Boolean = true,
) : ChatModelMiddleware {
    override suspend fun intercept(
        input: ChatModelInput,
        context: ExecutionContext,
        next: suspend (ChatModelInput, ExecutionContext) -> ModelOutput,
    ): ModelOutput {
        if (logPrompts) {
            logger.log("Invoking model with prompt: ${input.prompt}", MobileGraphLogger.Severity.INFO)
        }

        val startTime = currentTimeMillis()
        val result = next(input, context)
        val duration = currentTimeMillis() - startTime

        if (logOutputs) {
            when (result) {
                is ModelOutput.ChatOutput -> {
                    logger.log("Model response (${duration}ms): ${result.message.content}", MobileGraphLogger.Severity.INFO)
                }

                is ModelOutput.ErrorOutput -> {
                    logger.log("Model error (${duration}ms): ${result.error.message}", MobileGraphLogger.Severity.ERROR)
                }
            }
        }

        return result
    }

    // Simple helper for multiplatform time
    private fun currentTimeMillis(): Long =
        io.ktor.util.date
            .getTimeMillis()
}

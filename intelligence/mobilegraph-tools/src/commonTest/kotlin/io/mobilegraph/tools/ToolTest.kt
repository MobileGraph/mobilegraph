package io.mobilegraph.tools

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.tools.Tool
import io.mobilegraph.core.tools.ToolExecutionException
import io.mobilegraph.core.tools.ToolMetadata
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToolTest {
    private val testMetadata =
        ToolMetadata(
            name = "test-tool",
            description = "A simple tool for testing",
            tags = setOf("test"),
        )

    private class CalculatorTool(
        override val metadata: ToolMetadata,
    ) : Tool<Pair<Int, Int>, Int> {
        override suspend fun invoke(
            input: Pair<Int, Int>,
            context: io.mobilegraph.core.context.ExecutionContext,
        ): Int {
            if (input.first < 0) throw ToolExecutionException("Negative numbers not allowed")
            return input.first + input.second
        }
    }

    @Test
    fun testToolMetadata() {
        val tool = CalculatorTool(testMetadata)
        assertEquals("test-tool", tool.metadata.name)
        assertEquals("A simple tool for testing", tool.metadata.description)
        assertEquals(setOf("test"), tool.metadata.tags)
    }

    @Test
    fun testToolExecution() =
        runTest {
            val tool = CalculatorTool(testMetadata)
            val context =
                SimpleExecutionContext(
                    traceId = TraceId("t1"),
                    requestId = RequestId("r1"),
                )
            val result = tool.invoke(5 to 10, context)
            assertEquals(15, result)
        }

    @Test
    fun testToolErrorHandling() =
        runTest {
            val tool = CalculatorTool(testMetadata)
            val context =
                SimpleExecutionContext(
                    traceId = TraceId("t1"),
                    requestId = RequestId("r1"),
                )
            assertFailsWith<ToolExecutionException> {
                tool.invoke(-1 to 10, context)
            }
        }
}

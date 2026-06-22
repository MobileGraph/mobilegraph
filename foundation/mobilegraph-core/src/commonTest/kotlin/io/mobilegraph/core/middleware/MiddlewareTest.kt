package io.mobilegraph.core.middleware

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MiddlewareTest {
    @Test
    fun testMiddleware() =
        runTest {
            val context = SimpleExecutionContext(TraceId("t1"), requestId = RequestId("r1"))

            val middleware =
                object : Middleware<String, String> {
                    override suspend fun intercept(
                        input: String,
                        context: io.mobilegraph.core.context.ExecutionContext,
                        next: suspend (String, io.mobilegraph.core.context.ExecutionContext) -> String,
                    ): String = "intercepted:" + next(input, context)
                }

            val result = middleware.intercept("input", context) { i, _ -> i.uppercase() }
            assertEquals("intercepted:INPUT", result)
        }
}

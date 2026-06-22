/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.context

import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.core.metadata.Metadata
import io.mobilegraph.core.registry.ComponentProvider
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExecutionContextTest {
    @Test
    fun testExecutionContextProperties() {
        val context =
            SimpleExecutionContext(
                traceId = TraceId("t1"),
                requestId = RequestId("r1"),
                locale = "en-US",
            )

        assertEquals(TraceId("t1"), context.traceId)
        assertEquals(RequestId("r1"), context.requestId)
        assertEquals("en-US", context.locale)
    }

    @Test
    fun testWithMethods() {
        val context =
            SimpleExecutionContext(
                traceId = TraceId("t1"),
                requestId = RequestId("r1"),
            )

        val newContext = context.withRequestId(RequestId("r2"))
        assertEquals(RequestId("r2"), newContext.requestId)
        assertEquals(TraceId("t1"), newContext.traceId)

        val metaContext = context.withMetadata(Metadata(mapOf("foo" to "bar")))
        assertEquals("bar", metaContext.metadata["foo"])
    }

    @Test
    fun testComponentResolution() {
        val provider =
            object : ComponentProvider {
                @Suppress("UNCHECKED_CAST")
                override fun <T : Any> getComponent(clazz: KClass<T>): T? = if (clazz == String::class) "component" as T else null
            }

        val context =
            SimpleExecutionContext(
                traceId = TraceId("t1"),
                requestId = RequestId("r1"),
                componentProvider = provider,
            )

        assertEquals("component", context.getComponent(String::class))
        assertNull(context.getComponent(Int::class))
    }

    @Test
    fun testEmptyContext() {
        val empty = ExecutionContext.Empty
        assertEquals("empty-trace", empty.traceId.value)
        assertEquals("empty-req", empty.requestId.value)
        assertNull(empty.sessionId)
    }
}

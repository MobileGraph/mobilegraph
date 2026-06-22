/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.models.memory

import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.SessionId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.HumanMessage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryChatMessageHistoryTest {
    @Test
    fun testSessionIsolation() =
        runTest {
            val history = InMemoryChatMessageHistory()

            val context1 = SimpleExecutionContext(TraceId("t1"), SessionId("s1"), RequestId("r1"))
            val context2 = SimpleExecutionContext(TraceId("t2"), SessionId("s2"), RequestId("r2"))

            val msg1 = HumanMessage("hello from s1")
            val msg2 = HumanMessage("hello from s2")

            history.add(msg1, context1)
            history.add(msg2, context2)

            val history1 = history.get(context1)
            val history2 = history.get(context2)

            assertEquals(1, history1.size)
            assertEquals("hello from s1", history1[0].content)

            assertEquals(1, history2.size)
            assertEquals("hello from s2", history2[0].content)
        }

    @Test
    fun testClearSession() =
        runTest {
            val history = InMemoryChatMessageHistory()
            val context = SimpleExecutionContext(TraceId("t1"), SessionId("s1"), RequestId("r1"))

            history.add(HumanMessage("test"), context)
            assertEquals(1, history.get(context).size)

            history.clear(context)
            assertEquals(0, history.get(context).size)
        }

    @Test
    fun testClearAll() =
        runTest {
            val history = InMemoryChatMessageHistory()
            val context1 = SimpleExecutionContext(TraceId("t1"), SessionId("s1"), RequestId("r1"))
            val context2 = SimpleExecutionContext(TraceId("t2"), SessionId("s2"), RequestId("r2"))

            history.add(HumanMessage("m1"), context1)
            history.add(HumanMessage("m2"), context2)

            history.clearAll()

            assertEquals(0, history.get(context1).size)
            assertEquals(0, history.get(context2).size)
        }
}

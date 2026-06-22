package io.mobilegraph.core.ids

import kotlin.test.Test
import kotlin.test.assertEquals

class IdsTest {
    @Test
    fun testIds() {
        val traceId = TraceId("t1")
        assertEquals("t1", traceId.value)

        val sessionId = SessionId("s1")
        assertEquals("s1", sessionId.value)
    }
}

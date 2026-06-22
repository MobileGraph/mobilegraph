package io.mobilegraph.core.capability

import kotlin.test.Test
import kotlin.test.assertEquals

class CapabilityTest {
    @Test
    fun testCapabilities() {
        assertEquals("Streaming", Capability.Streaming.name)
        assertEquals("FunctionCalling", Capability.FunctionCalling.name)
    }
}

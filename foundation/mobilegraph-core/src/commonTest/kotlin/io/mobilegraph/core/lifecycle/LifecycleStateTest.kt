package io.mobilegraph.core.lifecycle

import kotlin.test.Test
import kotlin.test.assertEquals

class LifecycleStateTest {
    @Test
    fun testLifecycleState() {
        assertEquals("Foreground", LifecycleState.Foreground.name)
        assertEquals(LifecycleState.Background, LifecycleState.valueOf("Background"))
    }
}

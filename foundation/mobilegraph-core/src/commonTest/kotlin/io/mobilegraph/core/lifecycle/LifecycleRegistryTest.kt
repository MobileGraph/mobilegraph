package io.mobilegraph.core.lifecycle

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LifecycleRegistryTest {
    @Test
    fun testStateTransitions() =
        runTest {
            val registry = LifecycleRegistry()
            assertEquals(LifecycleState.Foreground, registry.currentState.value)

            registry.onEvent(LifecycleEvent.EnterBackground)
            assertEquals(LifecycleState.Background, registry.currentState.value)

            registry.onEvent(LifecycleEvent.NetworkLost)
            assertEquals(LifecycleState.Offline, registry.currentState.value)

            registry.onEvent(LifecycleEvent.Restored)
            assertEquals(LifecycleState.Restored, registry.currentState.value)

            registry.onEvent(LifecycleEvent.Suspended)
            assertEquals(LifecycleState.Suspended, registry.currentState.value)
        }
}

package io.mobilegraph.core.registry

import io.mobilegraph.core.annotations.InternalMobileGraphApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(InternalMobileGraphApi::class)
class CompositeComponentProviderTest {
    private interface TestComponent

    private class TestComponentImpl(
        val value: String,
    ) : TestComponent

    @Test
    fun testResolutionOrder() {
        val provider1 =
            SimpleComponentProvider().apply {
                register(TestComponent::class, TestComponentImpl("p1"))
            }
        val provider2 =
            SimpleComponentProvider().apply {
                register(TestComponent::class, TestComponentImpl("p2"))
            }

        val composite = CompositeComponentProvider(listOf(provider1, provider2))
        val component = composite.getComponent(TestComponent::class) as TestComponentImpl

        assertEquals("p1", component.value, "Should resolve from the first provider (shadowing)")
    }

    @Test
    fun testFallback() {
        val provider1 = SimpleComponentProvider()
        val provider2 =
            SimpleComponentProvider().apply {
                register(TestComponent::class, TestComponentImpl("p2"))
            }

        val composite = CompositeComponentProvider(listOf(provider1, provider2))
        val component = composite.getComponent(TestComponent::class) as TestComponentImpl

        assertEquals("p2", component.value, "Should fallback to the second provider")
    }

    @Test
    fun testNotFound() {
        val provider1 = SimpleComponentProvider()
        val provider2 = SimpleComponentProvider()

        val composite = CompositeComponentProvider(listOf(provider1, provider2))
        val component = composite.getComponent(TestComponent::class)

        assertNull(component, "Should return null if not found in any provider")
    }
}

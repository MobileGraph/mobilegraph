package io.mobilegraph.core.registry

import io.mobilegraph.core.annotations.InternalMobileGraphApi
import kotlin.reflect.KClass

/**
 * A [ComponentProvider] that delegates to a list of other providers in order.
 * This allows for component shadowing and fallback mechanisms.
 */
@InternalMobileGraphApi
class CompositeComponentProvider(
    private val providers: List<ComponentProvider>,
) : ComponentProvider {
    override fun <T : Any> getComponent(clazz: KClass<T>): T? {
        for (provider in providers) {
            val component = provider.getComponent(clazz)
            if (component != null) return component
        }
        return null
    }
}

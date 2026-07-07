package io.mobilegraph.core.registry

import io.mobilegraph.core.annotations.InternalMobileGraphApi
import kotlin.reflect.KClass

/**
 * A simple implementation of [ComponentProvider] that stores components in a map.
 */
@InternalMobileGraphApi
class SimpleComponentProvider : ComponentProvider {
    private val components = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> register(
        clazz: KClass<T>,
        component: T,
    ) {
        components[clazz] = component
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getComponent(clazz: KClass<T>): T? = components[clazz] as? T
}

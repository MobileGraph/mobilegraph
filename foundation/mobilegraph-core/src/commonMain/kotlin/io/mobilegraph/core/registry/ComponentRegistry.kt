package io.mobilegraph.core.registry

import kotlin.reflect.KClass

/**
 * Internal registry for resolving components within the MobileGraph runtime.
 */
internal class ComponentRegistry {
    private val components = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> register(
        clazz: KClass<T>,
        instance: T,
    ) {
        components[clazz] = instance
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(clazz: KClass<T>): T? = components[clazz] as? T
}

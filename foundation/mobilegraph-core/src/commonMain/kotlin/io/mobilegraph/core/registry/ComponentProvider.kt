package io.mobilegraph.core.registry

import kotlin.reflect.KClass

/**
 * Interface for resolving components.
 */
interface ComponentProvider {
    /**
     * Retrieves a component of the specified class.
     */
    fun <T : Any> getComponent(clazz: KClass<T>): T?
}

/**
 * Convenience extension for resolving components with reified types.
 */
inline fun <reified T : Any> ComponentProvider.getComponent(): T? = getComponent(T::class)

/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.runtime.MobileGraphRuntime
import io.mobilegraph.core.session.MobileGraphSession

/**
 * Main entry point for the MobileGraph SDK.
 *
 * This class provides access to the global configuration, runtime, and sessions.
 * It must be initialized before use via one of the `initialize` methods.
 */
class MobileGraph private constructor(
    /**
     * The environment configuration for this instance.
     */
    val environment: MobileGraphEnvironment,
    /**
     * The underlying runtime for executing operations.
     */
    internal val runtime: MobileGraphRuntime,
) {
    /**
     * Creates a new session for interaction with the SDK.
     *
     * @param modelName Optional name of a specific model to bind to this session.
     * @return A newly created [MobileGraphSession].
     */
    fun createSession(modelName: String? = null): MobileGraphSession = runtime.createSession(modelName)

    /**
     * Accesses a component from the environment.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getComponent(clazz: kotlin.reflect.KClass<T>): T? {
        if (clazz == io.mobilegraph.core.events.EventPublisher::class) {
            return runtime as T
        }
        return environment.getComponent(clazz)
    }

    companion object {
        private var _instance: MobileGraph? = null

        /**
         * The globally initialized MobileGraph instance.
         *
         * @throws IllegalStateException if the SDK has not been initialized.
         */
        val instance: MobileGraph
            get() = _instance ?: throw IllegalStateException("MobileGraph not initialized. Call initialize() first.")

        /**
         * The current version of the MobileGraph SDK.
         */
        const val VERSION = "0.1.0"

        /**
         * Initializes MobileGraph with the given environment.
         *
         * @param context Optional platform-specific context (e.g., Android Application Context).
         * @param environment The pre-built [MobileGraphEnvironment].
         * @return The initialized [MobileGraph] instance.
         */
        fun initialize(
            context: Any? = null,
            environment: MobileGraphEnvironment,
        ): MobileGraph {
            val runtime = MobileGraphRuntime(environment)
            return MobileGraph(environment, runtime).also { _instance = it }
        }

        /**
         * DSL for initializing MobileGraph.
         *
         * @param context Optional platform-specific context.
         * @param block Configuration block using [MobileGraphEnvironment.Builder].
         * @return The initialized [MobileGraph] instance.
         */
        fun initialize(
            context: Any? = null,
            block: MobileGraphEnvironment.Builder.() -> Unit,
        ): MobileGraph {
            val builder = MobileGraphEnvironment.Builder()
            builder.block()
            return initialize(context, builder.build())
        }

        /**
         * DSL for initializing MobileGraph using operator invoke syntax.
         *
         * @param block Configuration block using [MobileGraphEnvironment.Builder].
         * @return The initialized [MobileGraph] instance.
         */
        operator fun invoke(block: MobileGraphEnvironment.Builder.() -> Unit): MobileGraph = initialize(null, block)
    }
}

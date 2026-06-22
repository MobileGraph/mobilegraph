/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.environment

import io.mobilegraph.core.configuration.MobileGraphConfiguration
import io.mobilegraph.core.middleware.Middleware
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MobileGraphEnvironmentTest {
    @Test
    fun testComponentRegistration() {
        val environment =
            MobileGraphEnvironment
                .Builder()
                .component(String::class, "test-component")
                .build()

        assertEquals("test-component", environment.getComponent(String::class))
        assertNull(environment.getComponent(Int::class))
    }

    @Test
    fun testMiddlewareRegistration() {
        val mockMiddleware =
            object : Middleware<String, String> {
                override suspend fun intercept(
                    input: String,
                    context: io.mobilegraph.core.context.ExecutionContext,
                    next: suspend (String, io.mobilegraph.core.context.ExecutionContext) -> String,
                ): String = next(input, context)
            }

        val environment =
            MobileGraphEnvironment
                .Builder()
                .middleware(mockMiddleware)
                .build()

        assertEquals(1, environment.middleware.size)
        assertEquals(mockMiddleware, environment.middleware[0])
    }

    @Test
    fun testConfiguration() {
        val config = MobileGraphConfiguration()
        val environment =
            MobileGraphEnvironment
                .Builder()
                .configuration(config)
                .build()

        assertEquals(config, environment.configuration)
    }

    @Test
    fun testRuntimeConfig() {
        val environment =
            MobileGraphEnvironment
                .Builder()
                .runtime {
                    maxConcurrentExecutions = 10
                }.build()

        assertEquals(10, environment.runtimeConfig.maxConcurrentExecutions)
    }
}

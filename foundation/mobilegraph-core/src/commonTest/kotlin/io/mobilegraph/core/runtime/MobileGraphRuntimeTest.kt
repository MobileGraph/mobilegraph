/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.core.runtime

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MobileGraphRuntimeTest {
    @Test
    fun testSessionCreation() {
        val environment = MobileGraphEnvironment.Builder().build()
        val runtime = MobileGraphRuntime(environment)

        val session = runtime.createSession()
        assertNotNull(session)
        assertNotNull(session.sessionId)
        assertEquals(environment, session.environment)
    }

    @Test
    fun testEventPublishing() =
        runTest {
            val environment = MobileGraphEnvironment.Builder().build()
            val runtime = MobileGraphRuntime(environment)

            val event = MobileGraphEvent.RequestStarted(TraceId("t1"), RequestId("r1"))

            runtime.publishEvent(event)

            val receivedEvent = runtime.events.first()
            assertEquals(event, receivedEvent)
        }

    @Test
    fun testNamedSessionCreation() {
        val environment = MobileGraphEnvironment.Builder().build()
        val runtime = MobileGraphRuntime(environment)

        val session = runtime.createSession(modelName = "test-model")
        assertEquals("test-model", session.modelName)
    }
}

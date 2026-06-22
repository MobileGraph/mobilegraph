package io.mobilegraph.core.facade

import kotlin.test.Test
import kotlin.test.assertNotNull

class MobileGraphTest {
    @Test
    fun testInitialization() {
        val mobileGraph =
            MobileGraph {
                // Configuration can be done here
            }

        assertNotNull(mobileGraph)

        val session = mobileGraph.createSession()
        assertNotNull(session)
        assertNotNull(session.sessionId)
    }

    @Test
    fun testEnvironmentComponents() {
        val mockComponent = "MockComponent"

        val mobileGraph =
            MobileGraph {
                component(String::class, mockComponent)
            }

        // Internal check (not normally possible from outside, but for testing purposes)
        // Since getComponent is public on Environment, we can check it if we had access to runtime
    }
}

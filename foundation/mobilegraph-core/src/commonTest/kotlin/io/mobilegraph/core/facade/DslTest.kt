package io.mobilegraph.core.facade

import kotlin.test.Test
import kotlin.test.assertNotNull

class DslTest {
    @Test
    fun testFullDslStructure() {
        val mobileGraph =
            MobileGraph.initialize {
                runtime {
                    maxConcurrentExecutions = 5
                }

                models {
                    // Models will be registered here via extensions
                }

                knowledge {
                    // Vector stores
                }

                memory {
                    // History
                }

                tools {
                    // Actions
                }

                observability {
                    // Tracing
                }

                security {
                    // Encryption
                }

                storage {
                    // Persistence
                }

                lifecycle {
                    // Platform lifecycle
                }

                plugins {
                    // Custom extensions
                }
            }

        assertNotNull(mobileGraph)
    }
}

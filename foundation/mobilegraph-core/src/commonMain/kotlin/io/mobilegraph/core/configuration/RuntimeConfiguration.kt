package io.mobilegraph.core.configuration

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Configuration for the MobileGraph runtime execution environment.
 */
class RuntimeConfiguration {
    var coroutineScope: CoroutineScope? = null
    var dispatcher: CoroutineDispatcher = Dispatchers.Default
    var maxConcurrentExecutions: Int = 10
}

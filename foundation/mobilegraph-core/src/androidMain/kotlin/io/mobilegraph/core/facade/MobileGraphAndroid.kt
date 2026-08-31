package io.mobilegraph.core.facade

import android.content.Context
import io.mobilegraph.core.context.AndroidContext
import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.lifecycle.AndroidLifecycleObserver
import io.mobilegraph.core.lifecycle.LifecycleEvent
import io.mobilegraph.core.lifecycle.LifecycleRegistry

/**
 * Android-specific initialization for MobileGraph.
 *
 * @param context The Android context (application context will be extracted).
 * @param restored Whether this initialization is a restoration from a previous process death.
 * @param block Optional DSL configuration block.
 */
fun MobileGraph.Companion.initialize(
    context: Context,
    restored: Boolean = false,
    block: MobileGraphEnvironment.Builder.() -> Unit = {},
): MobileGraph {
    val builder = MobileGraphEnvironment.Builder()

    // Register Android Context
    builder.component(AndroidContext::class, AndroidContext(context.applicationContext))

    // Register LifecycleRegistry
    val registry = LifecycleRegistry()
    builder.component(LifecycleRegistry::class, registry)

    val observer = AndroidLifecycleObserver(context.applicationContext, registry)
    builder.component(io.mobilegraph.core.lifecycle.LifecycleObserver::class, observer)

    // Run custom configuration
    builder.block()

    val instance = initialize(builder.build())

    // If this is a restoration, emit the event after initialization
    if (restored) {
        registry.onEvent(LifecycleEvent.Restored)
    }

    // Start lifecycle observation
    observer.start()

    return instance
}

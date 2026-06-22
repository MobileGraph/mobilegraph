package io.mobilegraph.tools.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment
import io.mobilegraph.core.tools.ToolSelector

/**
 * Configures a custom tool selector.
 */
fun MobileGraphEnvironment.Builder.withToolSelector(selector: ToolSelector) =
    apply {
        component(ToolSelector::class, selector)
    }

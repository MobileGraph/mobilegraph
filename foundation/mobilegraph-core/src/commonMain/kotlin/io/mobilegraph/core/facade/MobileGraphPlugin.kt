package io.mobilegraph.core.facade

import io.mobilegraph.core.environment.MobileGraphEnvironment

/**
 * Interface for MobileGraph plugins.
 */
interface MobileGraphPlugin<TConfiguration : Any, TPlugin : Any> {
    /**
     * Installs the plugin into the environment.
     */
    fun install(
        environmentBuilder: MobileGraphEnvironment.Builder,
        configure: TConfiguration.() -> Unit,
    ): TPlugin
}

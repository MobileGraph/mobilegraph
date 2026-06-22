package io.mobilegraph.core.configuration

/**
 * Global configuration for the MobileGraph SDK.
 */
data class MobileGraphConfiguration(
    val timeoutMillis: Long = 30_000,
    val maxRetries: Int = 3,
    val debugMode: Boolean = false,
) {
    class Builder {
        var timeoutMillis: Long = 30_000
        var maxRetries: Int = 3
        var debugMode: Boolean = false

        fun build() =
            MobileGraphConfiguration(
                timeoutMillis = timeoutMillis,
                maxRetries = maxRetries,
                debugMode = debugMode,
            )
    }
}

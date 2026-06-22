package io.mobilegraph.core.lifecycle

/**
 * Represents the lifecycle state of the application or runtime.
 */
enum class LifecycleState {
    Foreground,
    Background,
    Offline,
    Suspended,
    Killed,
    Restored,
}

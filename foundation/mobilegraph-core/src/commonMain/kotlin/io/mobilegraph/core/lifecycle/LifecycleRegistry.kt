package io.mobilegraph.core.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registry that manages the global [LifecycleState] and broadcasts [LifecycleEvent]s.
 */
class LifecycleRegistry {
    private val _currentState = MutableStateFlow(LifecycleState.Foreground)

    /**
     * The current lifecycle state of the application.
     */
    val currentState: StateFlow<LifecycleState> = _currentState.asStateFlow()

    /**
     * Updates the current state and processes the corresponding event.
     */
    fun onEvent(event: LifecycleEvent) {
        val newState =
            when (event) {
                LifecycleEvent.EnterForeground -> LifecycleState.Foreground

                LifecycleEvent.EnterBackground -> LifecycleState.Background

                LifecycleEvent.NetworkLost -> LifecycleState.Offline

                LifecycleEvent.NetworkRestored -> LifecycleState.Foreground

                // Or previous state
                LifecycleEvent.Suspended -> LifecycleState.Suspended

                LifecycleEvent.Restored -> LifecycleState.Restored
            }

        _currentState.value = newState
        // Future: Emit to a shared flow of events if needed for side-effects
    }
}

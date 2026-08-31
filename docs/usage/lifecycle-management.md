# Lifecycle Management & Durable Execution

MobileGraph is designed to be "Mobile-First," meaning it natively understands and reacts to the volatile nature of mobile application lifecycles (foregrounding, backgrounding, process death, and connectivity changes).

## Core Concepts

### Lifecycle States
The SDK tracks the following states via the `LifecycleRegistry`:
*   **Foreground**: App is visible and active.
*   **Background**: App is no longer visible.
*   **Offline**: Network connectivity is lost.
*   **Suspended**: System is under memory pressure or preparing to freeze the process.
*   **Restored**: SDK has been re-initialized after process death.

### Durable Execution
MobileGraph uses a **Checkpointing System**. Every time a node in a `StateGraph` completes, its state is persisted to a `CheckpointStore`. This ensures that execution can be resumed even if the app is killed by the OS.

---

## Initialization (Android)

To enable automatic lifecycle tracking on Android, use the Android-specific initializer:

```kotlin
// In your Application or MainActivity
MobileGraph.initialize(context) {
    // Standard configuration
}
```

To handle **Process Death Restoration**, pass the `restored` flag:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val isRestoring = savedInstanceState != null
    MobileGraph.initialize(this, restored = isRestoring)
}
```

### Note on `AndroidLifecycleObserver`
When using the Android initializer, the SDK automatically registers an `AndroidLifecycleObserver`. This internal component manages several critical platform bridges:

1.  **Process Lifecycle**: Maps `androidx.lifecycle` events to `Foreground` and `Background` states.
2.  **Connectivity Monitoring**: Uses `ConnectivityManager` to detect network loss and restoration (Requires `android.permission.ACCESS_NETWORK_STATE`).
3.  **Memory Pressure**: Implements `ComponentCallbacks2` to trigger the `Suspended` state when the system is aggressively reclaiming memory, ensuring a final checkpoint is saved before potential process death.

The observer is registered as a `LifecycleObserver` component in the environment and is cleaned up automatically when `MobileGraph.terminate()` is called.

---

## Handling Background Scenarios

Developers can choose how workflows behave when the app enters the background using `BackgroundPolicy`.

### Approach 1: Background Policy (Automated)
When starting a graph execution, provide an `ExecutionConfig`.

```kotlin
val config = ExecutionConfig(backgroundPolicy = BackgroundPolicy.PAUSE)

agentRuntime.run(workflowGraph, initialState, config)
```

| Policy | Behavior |
| :--- | :--- |
| `CONTINUE` | (Default) Execution continues in the background. |
| `PAUSE` | Automatically saves a checkpoint and exits the loop when backgrounded. |
| `CANCEL` | Stops execution immediately and discards progress. |

### Approach 2: Manual Control (ViewModel)
You can observe the lifecycle registry directly to pause or cancel executions based on your own logic.

```kotlin
viewModelScope.launch {
    val registry = MobileGraph.instance.getComponent(LifecycleRegistry::class)
    registry?.currentState?.collect { state ->
        if (state == LifecycleState.Background) {
            agentRuntime.pause()
        }
    }
}
```

### Approach 3: Background Blocking (Middleware)
To prevent ANY model calls from triggering while the app is in the background, use a global middleware:

```kotlin
class BackgroundBlockerMiddleware : ChatModelMiddleware {
    override suspend fun intercept(input: ChatModelInput, context: ExecutionContext, next: ...) {
        if (context.lifecycleState == LifecycleState.Background) {
            return ModelOutput.ErrorOutput(Exception("Blocked in background"))
        }
        return next(input, context)
    }
}
```

---

## Foreground Resumption Strategy

If you use `BackgroundPolicy.PAUSE`, you likely want the workflow to resume as soon as the user returns to the app.

### Auto-Resume Pattern
When the engine pauses due to a background event, it returns an `ExecutionResult.AwaitingReview` containing a `checkpointId` suffixed with `_auto`.

```kotlin
// 1. In your ViewModel, keep track of the paused result
var awaitingResume by mutableStateOf<ExecutionResult.AwaitingReview?>(null)

// 2. Observe foreground transition
viewModelScope.launch {
    MobileGraph.instance.getComponent(LifecycleRegistry::class)?.currentState?.collect { state ->
        if (state == LifecycleState.Foreground) {
            awaitingResume?.let { review ->
                if (review.checkpointId?.endsWith("_auto") == true) {
                    resumeExecution(review)
                }
            }
        }
    }
}

// 3. Resume using the checkpoint
private suspend fun resumeExecution(review: ExecutionResult.AwaitingReview) {
    agentRuntime.resume(
        graph = myGraph,
        checkpointId = review.checkpointId!!,
        nodeId = review.nodeId
    )
}
```

---

## Best Practices

1.  **Always use a CheckpointStore**: Without it, background pausing is just a cancellation. Use `InMemoryCheckpointStore` for simple apps or a SQL-based store for true durability.
2.  **Use `PAUSE` for long-running agents**: If an agent takes 30+ seconds to complete a multi-step task, users will often switch apps. `PAUSE` ensures their progress (and your LLM costs) aren't wasted.
3.  **Check Lifecycle in Custom Nodes**: If you write a custom `GraphNode` that performs heavy local processing (e.g., image analysis), check `state.executionContext.lifecycleState` to decide if you should yield or pause.

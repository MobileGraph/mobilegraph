package io.mobilegraph.a2a.client

/**
 * Configuration for the [A2AClient].
 *
 * Controls timeouts, retry behavior, method name conventions, and payload limits
 * for all A2A communication. Designed with mobile/edge constraints in mind
 * (intermittent connectivity, battery, bandwidth).
 *
 * @property connectionTimeoutMs Maximum time to establish a connection (default 30s).
 * @property requestTimeoutMs Maximum time to wait for a response (default 120s).
 * @property socketTimeoutMs Maximum inactivity time between data packets (default 120s).
 * @property taskPollingIntervalMs Interval between task status polls.
 * @property streamReconnectDelayMs Delay before attempting to reconnect a stream.
 * @property maxRetries Maximum number of retries for idempotent operations.
 * @property retryDelayMs Base delay between retries (with exponential backoff).
 * @property maxPayloadBytes Maximum accepted response payload size.
 * @property useLegacyMethodNames Whether to use A2A v0.3 method names (`message/send`, `message/stream`, `tasks/get`, `tasks/cancel`). Default is `false` (uses A2A v1.0 `SendMessage`, `SendStreamingMessage`, `GetTask`, `CancelTask`).
 * @property autoFallbackToLegacy Whether to automatically retry using legacy v0.3 method names if the remote agent returns -32601 ("Method not found"). Default is `true`.
 */
data class A2AClientConfig(
    val connectionTimeoutMs: Long = 30_000L,
    val requestTimeoutMs: Long = 120_000L,
    val socketTimeoutMs: Long = 120_000L,
    val taskPollingIntervalMs: Long = 2_000L,
    val streamReconnectDelayMs: Long = 1_000L,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1_000L,
    val maxPayloadBytes: Long = 10_485_760L, // 10 MB
    val useLegacyMethodNames: Boolean = false,
    val autoFallbackToLegacy: Boolean = true,
)

package io.mobilegraph.a2a.client

/**
 * Helper object mapping A2A v1.0 and legacy v0.3 JSON-RPC method names.
 */
object A2AMethods {
    const val SEND_MESSAGE = "SendMessage"
    const val SEND_STREAMING_MESSAGE = "SendStreamingMessage"
    const val STREAM_MESSAGE = "StreamMessage"
    const val GET_TASK = "GetTask"
    const val CANCEL_TASK = "CancelTask"

    const val LEGACY_SEND_MESSAGE = "message/send"
    const val LEGACY_STREAM_MESSAGE = "message/stream"
    const val LEGACY_GET_TASK = "tasks/get"
    const val LEGACY_CANCEL_TASK = "tasks/cancel"

    fun resolve(
        v1Method: String,
        useLegacy: Boolean,
    ): String {
        if (!useLegacy) return v1Method
        return when (v1Method) {
            SEND_MESSAGE -> LEGACY_SEND_MESSAGE
            SEND_STREAMING_MESSAGE, STREAM_MESSAGE -> LEGACY_STREAM_MESSAGE
            GET_TASK -> LEGACY_GET_TASK
            CANCEL_TASK -> LEGACY_CANCEL_TASK
            else -> v1Method
        }
    }

    fun toLegacy(v1Method: String): String =
        when (v1Method) {
            SEND_MESSAGE -> LEGACY_SEND_MESSAGE
            SEND_STREAMING_MESSAGE, STREAM_MESSAGE -> LEGACY_STREAM_MESSAGE
            GET_TASK -> LEGACY_GET_TASK
            CANCEL_TASK -> LEGACY_CANCEL_TASK
            else -> v1Method
        }
}

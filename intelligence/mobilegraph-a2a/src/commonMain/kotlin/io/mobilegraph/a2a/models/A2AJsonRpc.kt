package io.mobilegraph.a2a.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A2A JSON-RPC 2.0 request envelope.
 *
 * Used for all A2A protocol operations:
 * - `message/send`
 * - `message/stream`
 * - `tasks/get`
 * - `tasks/cancel`
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class A2ARequest(
    @EncodeDefault val jsonrpc: String = "2.0",
    val id: JsonElement,
    val method: String,
    val params: JsonObject? = null,
)

/**
 * A2A JSON-RPC 2.0 response envelope.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class A2AResponse(
    @EncodeDefault val jsonrpc: String = "2.0",
    val id: JsonElement?,
    val result: JsonElement? = null,
    val error: A2AJsonRpcError? = null,
)

/**
 * A2A JSON-RPC 2.0 error object.
 *
 * Standard error codes:
 * - `-32700` Parse error
 * - `-32600` Invalid request
 * - `-32601` Method not found
 * - `-32602` Invalid params
 * - `-32603` Internal error
 * - `-32001` Task not found
 * - `-32002` Task not cancelable
 * - `-32003` Push notification not supported
 * - `-32004` Unsupported operation
 */
@Serializable
data class A2AJsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null,
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
        const val TASK_NOT_FOUND = -32001
        const val TASK_NOT_CANCELABLE = -32002
        const val PUSH_NOTIFICATION_NOT_SUPPORTED = -32003
        const val UNSUPPORTED_OPERATION = -32004
    }
}

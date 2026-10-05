package io.mobilegraph.a2a.errors

import io.mobilegraph.core.exceptions.MobileGraphException

/**
 * Base exception for all A2A protocol errors.
 *
 * All A2A-specific exceptions extend this class, which itself extends
 * [MobileGraphException] to integrate with MobileGraph's error hierarchy.
 */
open class A2AException(
    message: String,
    cause: Throwable? = null,
) : MobileGraphException(message, cause)

/**
 * Thrown when Agent Card discovery fails.
 *
 * Possible causes:
 * - Agent not reachable
 * - Invalid Agent Card JSON
 * - Missing `/.well-known/agent-card.json`
 */
class A2ADiscoveryException(
    message: String,
    cause: Throwable? = null,
) : A2AException(message, cause)

/**
 * Thrown when authentication with a remote A2A agent fails.
 */
class A2AAuthenticationException(
    message: String,
    cause: Throwable? = null,
) : A2AException(message, cause)

/**
 * Thrown when the remote agent returns a JSON-RPC protocol error.
 *
 * @property errorCode The JSON-RPC error code from the response.
 */
class A2AProtocolException(
    message: String,
    val errorCode: Int? = null,
    cause: Throwable? = null,
) : A2AException(message, cause)

/**
 * Thrown when a requested task is not found on the remote agent.
 *
 * @property taskId The task ID that was not found.
 */
class A2ATaskNotFoundException(
    val taskId: String,
) : A2AException("A2A task not found: $taskId")

/**
 * Thrown when an A2A operation times out.
 *
 * Can occur during:
 * - Connection establishment
 * - Request/response
 * - Task polling
 * - Streaming
 */
class A2ATimeoutException(
    message: String,
    cause: Throwable? = null,
) : A2AException(message, cause)

/**
 * Thrown when an SSE streaming error occurs.
 *
 * Can occur due to:
 * - Connection interruption
 * - Malformed SSE events
 * - Server-side stream failure
 */
class A2AStreamException(
    message: String,
    cause: Throwable? = null,
) : A2AException(message, cause)

/**
 * Thrown when A2A request/response validation fails.
 *
 * Can occur due to:
 * - Malformed Agent Card
 * - Invalid task state transitions
 * - Unsupported content types
 * - Payload size exceeded
 */
class A2AValidationException(
    message: String,
) : A2AException(message)

/**
 * Thrown when the remote agent does not support a requested operation.
 *
 * @property operation The unsupported operation name.
 */
class A2AUnsupportedOperationException(
    val operation: String,
) : A2AException("Unsupported A2A operation: $operation")

/**
 * Thrown when a network-level failure occurs during A2A communication.
 *
 * Wraps underlying transport errors into a stable A2A error type
 * so that application code does not need to handle raw networking exceptions.
 */
class A2ANetworkException(
    message: String,
    cause: Throwable? = null,
) : A2AException(message, cause)

package io.mobilegraph.a2a.client

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.A2AJsonRpcError
import io.mobilegraph.a2a.models.A2AResponse
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.A2ATaskStatus
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Handles reading and processing SSE stream events for A2A communication.
 */
class A2AStreamProcessor(
    private val json: Json,
    private val responseParser: A2AResponseParser,
) {
    /**
     * Reads lines from a ByteReadChannel and parses individual SSE event blocks.
     */
    suspend fun processStreamEvents(
        channel: ByteReadChannel,
        collector: FlowCollector<A2AStreamEvent>,
    ) {
        var currentEvent: String? = null
        var currentData = StringBuilder()

        while (true) {
            val line =
                try {
                    channel.readUTF8Line() ?: break
                } catch (e: Exception) {
                    collector.emit(
                        A2AStreamEvent.StreamError(
                            A2AJsonRpcError(
                                code = A2AJsonRpcError.INTERNAL_ERROR,
                                message = "Stream read error: ${e.message}",
                            ),
                        ),
                    )
                    break
                }

            if (line.isEmpty()) {
                if (currentData.isNotEmpty()) {
                    processStreamEvent(currentEvent, currentData.toString(), collector)
                    currentData = StringBuilder()
                    currentEvent = null
                }
                continue
            }

            when {
                line.startsWith("event:") -> {
                    currentEvent = line.substring(6).trim()
                }

                line.startsWith("data:") -> {
                    val data = line.substring(5).trim()
                    if (currentData.isNotEmpty()) {
                        currentData.append("\n")
                    }
                    currentData.append(data)
                }

                line.startsWith(":") -> {
                    // SSE comment
                }
            }
        }

        if (currentData.isNotEmpty()) {
            processStreamEvent(currentEvent, currentData.toString(), collector)
        }
    }

    /**
     * Parses an individual SSE data payload and emits the corresponding [A2AStreamEvent].
     */
    suspend fun processStreamEvent(
        eventType: String?,
        data: String,
        collector: FlowCollector<A2AStreamEvent>,
    ) {
        try {
            val response = json.decodeFromString<A2AResponse>(data)

            response.error?.let { error ->
                collector.emit(A2AStreamEvent.StreamError(error))
                return
            }

            val result = response.result ?: return
            val resultObj =
                (result as? JsonObject) ?: (
                    try {
                        result.jsonObject
                    } catch (_: Exception) {
                        null
                    }
                )

            // 1. Unwrap A2A v1.0 SSE wrapper keys ("statusUpdate", "artifactUpdate", "task")
            val payloadObj =
                when {
                    resultObj != null && "statusUpdate" in resultObj && resultObj["statusUpdate"] is JsonObject -> {
                        resultObj["statusUpdate"]!!.jsonObject
                    }

                    resultObj != null && "artifactUpdate" in resultObj && resultObj["artifactUpdate"] is JsonObject -> {
                        resultObj["artifactUpdate"]!!.jsonObject
                    }

                    resultObj != null && "task" in resultObj && resultObj["task"] is JsonObject -> {
                        resultObj["task"]!!.jsonObject
                    }

                    else -> {
                        resultObj
                    }
                }

            if (payloadObj != null) {
                val normalizedPayload = responseParser.normalizeTaskId(payloadObj)

                // 2. Try to parse normalizedPayload as a full task ONLY if explicitly formatted as a complete task
                val isPartialUpdate =
                    "artifact" in normalizedPayload ||
                        (
                            "status" in normalizedPayload && !normalizedPayload.containsKey("artifacts") &&
                                !normalizedPayload.containsKey("history")
                        )

                if (!isPartialUpdate && normalizedPayload.containsKey("status")) {
                    try {
                        val task = json.decodeFromJsonElement<A2ATask>(normalizedPayload)
                        if (task.id.isNotBlank()) {
                            if (task.status.state.isTerminal()) {
                                collector.emit(A2AStreamEvent.TaskComplete(task))
                            } else {
                                collector.emit(
                                    A2AStreamEvent.TaskStatusUpdate(
                                        taskId = task.id,
                                        status = task.status,
                                        isFinal = false,
                                    ),
                                )
                                if (task.artifacts.isNotEmpty()) {
                                    task.artifacts.forEach { artifact ->
                                        collector.emit(
                                            A2AStreamEvent.TaskArtifactUpdate(
                                                taskId = task.id,
                                                artifact = artifact,
                                            ),
                                        )
                                    }
                                }
                            }
                            return
                        }
                    } catch (_: Exception) {
                        // Not a full task, try partial parsing below
                    }
                }

                // 3. Extract taskId and status/artifact updates from normalizedPayload
                val taskId =
                    normalizedPayload["id"]?.let { if (it is JsonPrimitive) it.content else null }
                        ?: normalizedPayload["taskId"]?.let { if (it is JsonPrimitive) it.content else null }
                        ?: normalizedPayload["task_id"]?.let { if (it is JsonPrimitive) it.content else null }
                        ?: ""

                // Check for status update
                normalizedPayload["status"]?.let { statusElement ->
                    val status =
                        try {
                            json.decodeFromJsonElement<A2ATaskStatus>(statusElement)
                        } catch (_: Exception) {
                            A2ATaskStatus(state = A2ATaskState.COMPLETED)
                        }
                    collector.emit(
                        A2AStreamEvent.TaskStatusUpdate(
                            taskId = taskId,
                            status = status,
                            isFinal = status.state.isTerminal(),
                        ),
                    )
                }

                // Check for single artifact update
                normalizedPayload["artifact"]?.let { artifactElement ->
                    val artifact = json.decodeFromJsonElement<A2AArtifact>(artifactElement)
                    collector.emit(
                        A2AStreamEvent.TaskArtifactUpdate(
                            taskId = taskId,
                            artifact = artifact,
                        ),
                    )
                }

                // Check for artifacts list update
                normalizedPayload["artifacts"]?.let { artifactsElement ->
                    val artifacts =
                        try {
                            json.decodeFromJsonElement<List<A2AArtifact>>(artifactsElement)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    artifacts.forEach { artifact ->
                        collector.emit(
                            A2AStreamEvent.TaskArtifactUpdate(
                                taskId = taskId,
                                artifact = artifact,
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            collector.emit(
                A2AStreamEvent.StreamError(
                    A2AJsonRpcError(
                        code = A2AJsonRpcError.INTERNAL_ERROR,
                        message = "Failed to parse SSE event data: ${e.message}",
                    ),
                ),
            )
        }
    }
}

package io.mobilegraph.a2a.client

import io.mobilegraph.a2a.errors.A2AProtocolException
import io.mobilegraph.a2a.models.A2AArtifact
import io.mobilegraph.a2a.models.A2AResponse
import io.mobilegraph.a2a.models.A2ATask
import io.mobilegraph.a2a.models.A2ATaskState
import io.mobilegraph.a2a.models.A2ATaskStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.random.Random

/**
 * Handles parsing and extraction of A2A JSON-RPC responses and task objects.
 * Supports both direct A2ATask payloads and A2A v1.0 wrapper patterns (`task`, `statusUpdate`, `artifactUpdate`).
 */
class A2AResponseParser(
    private val json: Json,
) {
    /**
     * Parses a JSON-RPC response result into an [A2ATask].
     * Handles top-level task objects, A2A v1.0 wrapped `{ "task": { ... } }` objects,
     * and fallback property extraction.
     *
     * @param response The JSON-RPC response wrapper.
     * @return The parsed [A2ATask].
     * @throws A2AProtocolException if the response cannot be parsed into a task.
     */
    fun parseTaskResponse(response: A2AResponse): A2ATask {
        val result =
            response.result ?: throw A2AProtocolException(
                "A2A response missing 'result' field",
            )

        // 1. Try decoding direct top-level A2ATask
        try {
            val task = json.decodeFromJsonElement<A2ATask>(result)
            if (task.id.isNotBlank()) {
                return task
            }
        } catch (_: Exception) {
        }

        // 2. Check if wrapped inside a "task" property (A2A v1.0 wrapper object)
        val resultObj =
            (result as? JsonObject) ?: (
                try {
                    result.jsonObject
                } catch (_: Exception) {
                    null
                }
            )

        if (resultObj != null) {
            val taskElement = resultObj["task"] ?: resultObj
            val normalizedTaskObj = normalizeTaskId(taskElement)

            try {
                val task = json.decodeFromJsonElement<A2ATask>(normalizedTaskObj)
                if (task.id.isNotBlank()) return task
            } catch (_: Exception) {
            }

            // 3. Fallback extraction if ID / status are structured differently
            val taskId =
                resultObj["id"]?.let { if (it is JsonPrimitive) it.content else null }
                    ?: resultObj["taskId"]?.let { if (it is JsonPrimitive) it.content else null }
                    ?: resultObj["task_id"]?.let { if (it is JsonPrimitive) it.content else null }
                    ?: "task-${Random.nextInt(1000, 9999)}"

            val status =
                resultObj["status"]?.let {
                    try {
                        json.decodeFromJsonElement<A2ATaskStatus>(it)
                    } catch (_: Exception) {
                        null
                    }
                } ?: A2ATaskStatus(state = A2ATaskState.COMPLETED)

            val artifacts =
                resultObj["artifacts"]?.let {
                    try {
                        json.decodeFromJsonElement<List<A2AArtifact>>(it)
                    } catch (_: Exception) {
                        emptyList()
                    }
                } ?: emptyList()

            val contextId =
                resultObj["contextId"]?.let { if (it is JsonPrimitive) it.content else null }
                    ?: resultObj["context_id"]?.let { if (it is JsonPrimitive) it.content else null }

            return A2ATask(
                id = taskId,
                contextId = contextId,
                status = status,
                artifacts = artifacts,
                metadata = resultObj,
            )
        }

        throw A2AProtocolException(
            "Failed to parse A2A task from response: $result",
        )
    }

    /**
     * Normalizes a JSON element so that `"taskId"` or `"task_id"` is mapped to `"id"` if missing.
     */
    fun normalizeTaskId(element: JsonElement): JsonObject {
        if (element is JsonObject) {
            if (!element.containsKey("id") && (element.containsKey("taskId") || element.containsKey("task_id"))) {
                val extractedId = element["taskId"] ?: element["task_id"]
                return JsonObject(element.toMutableMap().apply { if (extractedId != null) put("id", extractedId) })
            }
            return element
        }
        return JsonObject(emptyMap())
    }
}

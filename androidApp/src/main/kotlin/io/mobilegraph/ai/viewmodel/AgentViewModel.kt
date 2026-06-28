package io.mobilegraph.ai.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.mobilegraph.core.context.SimpleExecutionContext
import io.mobilegraph.core.events.MobileGraphEvent
import io.mobilegraph.core.facade.MobileGraph
import io.mobilegraph.core.facade.events
import io.mobilegraph.core.ids.RequestId
import io.mobilegraph.core.ids.TraceId
import io.mobilegraph.models.ModelOutput
import io.mobilegraph.models.facade.chat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class AgentViewModel : ViewModel() {
    var uiState by mutableStateOf("Ready")
    var isLoading by mutableStateOf(false)
    var currentOutput by mutableStateOf("")

    private val _eventLog = MutableStateFlow<List<String>>(emptyList())
    val eventLog: StateFlow<List<String>> = _eventLog

    init {
        viewModelScope.launch {
            MobileGraph.events.collect { event ->
                val message =
                    when (event) {
                        is MobileGraphEvent.RequestStarted -> "Agent request started"
                        is MobileGraphEvent.RequestCompleted -> "Agent request completed"
                        is MobileGraphEvent.RequestFailed -> "Agent request failed: ${event.errorMessage}"
                        else -> null
                    }
                message?.let { addEvent(it) }
            }
        }
    }

    fun runAgentChat(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) return@launch
            isLoading = true
            uiState = "Agent Thinking..."
            try {
                val context =
                    SimpleExecutionContext(
                        traceId = TraceId("agent-trace-${Random.nextInt()}"),
                        requestId = RequestId("agent-req-${Random.nextInt()}"),
                    )

                // Using the session chat to test agentic behavior if tools are configured
                val session = MobileGraph.instance.createSession()
                val response = session.chat(query, context = context)

                currentOutput = (response as? ModelOutput.ChatOutput)?.message?.content ?: "No response content"
                uiState = "Agent Replied"
                addEvent("Agent Query: $query")
            } catch (e: Exception) {
                uiState = "Agent Error: ${e.message}"
                addEvent("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private fun addEvent(event: String) {
        _eventLog.value = _eventLog.value + event
    }
}

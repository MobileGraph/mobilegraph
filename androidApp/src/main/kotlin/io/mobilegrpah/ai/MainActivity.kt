/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegrpah.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.mobilegrpah.ai.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel.initializeSdk(applicationContext)

        setContent {
            MaterialTheme {
                sampleAppScaffold(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun sampleAppScaffold(viewModel: MainViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MobileGraph SDK Sample") })
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            statusCard(viewModel.uiState, viewModel.isLoading, viewModel.currentSequenceItem)

            Spacer(modifier = Modifier.height(24.dp))

            sessionSwitchSection(
                currentSessionId = viewModel.currentSessionId,
                onSessionSelected = { viewModel.switchSession(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            featureSection(
                title = "1. Session-Scoped Chat",
                description = "Uses a specific session model and history. Best for Conversational UIs.",
                buttonText = "Run Session Chat",
                onRun = { viewModel.runSessionChat("Hello! My name is Alice.") },
            )

            featureSection(
                title = "1b. Streaming Chat",
                description = "Demonstrates token-by-token streaming within a session.",
                buttonText = "Run Streaming Chat",
                onRun = { viewModel.runStreamingChat("Tell me a story about a brave mobile developer in 3 paragraphs.") },
            )

            featureSection(
                title = "1b. Sequential Chat Test",
                description = "Runs a list of queries one by one to test conversation context.",
                buttonText = "Run Chat Sequence",
                onRun = { viewModel.runChatSequence() },
            )

            featureSection(
                title = "2. Global-Scoped Task",
                description = "Uses the global default model. No session history attached. Best for Utility Tasks.",
                buttonText = "Run Global Task",
                onRun = { viewModel.runGlobalUtilityTask("This is a long text that needs to be summarized into one sentence.") },
            )

            Button(
                onClick = { viewModel.clearGlobalHistory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.filledTonalButtonColors(),
            ) {
                Text("Clear Global History")
            }

            featureSection(
                title = "3. Structured Output",
                description = "Extract type-safe data from raw AI text using session context.",
                buttonText = "Parse Review",
                onRun = { viewModel.runStructuredParsing("The iPhone 15 has great battery and camera, but it is expensive. Rating 4/5.") },
            )

            Spacer(modifier = Modifier.height(24.dp))

            eventLogSection(viewModel.eventLog)

            Spacer(modifier = Modifier.height(24.dp))

            outputCard(viewModel.currentOutput)
        }
    }
}

@Composable
fun statusCard(
    status: String,
    isLoading: Boolean,
    currentAction: String = "",
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("System Status", fontWeight = FontWeight.Bold)
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Text("$status (scroll down for output)", style = MaterialTheme.typography.bodySmall)
            if (currentAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Current Action:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                Text(currentAction, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun sessionSwitchSection(
    currentSessionId: String,
    onSessionSelected: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Active Conversation Session", fontWeight = FontWeight.Bold)
            Text("Switching sessions isolates history and models.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Session 1", "Session 2").forEach { id ->
                    val isSelected = currentSessionId == id
                    Button(
                        onClick = { onSessionSelected(id) },
                        colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(id)
                    }
                }
            }
        }
    }
}

@Composable
fun featureSection(
    title: String,
    description: String,
    buttonText: String,
    onRun: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRun, modifier = Modifier.fillMaxWidth()) {
            Text(buttonText)
        }
    }
}

@Composable
fun eventLogSection(events: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Live Event Log (Passive Observability)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text("No events yet. Start a chat to see lifecycle events.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                events.forEach { event ->
                    Text("• $event", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun outputCard(output: String) {
    if (output.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Output", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(output, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

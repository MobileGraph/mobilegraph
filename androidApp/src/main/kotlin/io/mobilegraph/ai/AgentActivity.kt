package io.mobilegraph.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mobilegraph.ai.viewmodel.AgentViewModel

class AgentActivity : ComponentActivity() {
    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AgentScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun AgentScreen(viewModel: AgentViewModel) {
    var query by remember { mutableStateOf("") }
    val eventLog by viewModel.eventLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Phase 3: Agents & Tools") })
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
            Text("Status: ${viewModel.uiState}", style = MaterialTheme.typography.titleMedium)

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Agent Query (e.g. 'What is the weather in London?')") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.runAgentChat(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && query.isNotBlank(),
            ) {
                Text("Run Agent Chat")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.currentOutput.isNotEmpty()) {
                Text("Response:", style = MaterialTheme.typography.titleSmall)
                Text(viewModel.currentOutput, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Event Log:", style = MaterialTheme.typography.titleSmall)
            eventLog.forEach { event ->
                Text("• $event", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

package io.mobilegraph.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.mobilegraph.ai.viewmodel.ToolsAgentViewModel

class ToolsAgentActivity : ComponentActivity() {
    private val viewModel: ToolsAgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.initializeSdk(applicationContext)
        setContent {
            MaterialTheme {
                ToolsAgentScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun ToolsAgentScreen(viewModel: ToolsAgentViewModel) {
    var query by remember { mutableStateOf("What is the weather in London?") }
    val eventLog by viewModel.eventLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Autonomous Tool Agent") })
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
            Text("Agent Status: ${viewModel.uiState}", style = MaterialTheme.typography.titleMedium)

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Ask the Agent something (e.g. weather, math)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.runAgent(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && query.isNotBlank(),
            ) {
                Text("Send to Agent")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.agentResponse.isNotEmpty()) {
                Text("Agent Response:", fontWeight = FontWeight.Bold)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(12.dp),
                ) {
                    Text(viewModel.agentResponse)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Internal Log:", style = MaterialTheme.typography.titleSmall)
            eventLog.reversed().forEach { event ->
                Text("• $event", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

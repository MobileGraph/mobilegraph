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
import androidx.compose.material3.HorizontalDivider
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
import io.mobilegraph.ai.viewmodel.ParallelAgentsViewModel

class ParallelAgentsActivity : ComponentActivity() {
    private val viewModel: ParallelAgentsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.initializeSdk(applicationContext)
        setContent {
            MaterialTheme {
                ParallelAgentsScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun ParallelAgentsScreen(viewModel: ParallelAgentsViewModel) {
    var topic by remember { mutableStateOf("The impact of quantum computing") }
    val eventLog by viewModel.eventLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Parallel Agents (Fan-out/in)") })
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
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic for Analysis") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.runParallelWorkflow(topic) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && topic.isNotBlank(),
            ) {
                Text("Execute Parallel Workflow")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Researcher Section
            if (viewModel.researchOutput.isNotEmpty()) {
                SectionTitle("Researcher Agent Result")
                ContentBox(viewModel.researchOutput, Color(0xFFE3F2FD))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Creative Section
            if (viewModel.creativeOutput.isNotEmpty()) {
                SectionTitle("Creative Writer Agent Result")
                ContentBox(viewModel.creativeOutput, Color(0xFFF3E5F5))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Final Integrated Section
            if (viewModel.finalSummary.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionTitle("Final Aggregated Status")
                Text(viewModel.finalSummary, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Event Log (Real-time Execution):", style = MaterialTheme.typography.titleSmall)
            eventLog.reversed().forEach { event ->
                Text("• $event", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
@Suppress("ktlint:standard:function-naming")
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
@Suppress("ktlint:standard:function-naming")
fun ContentBox(
    content: String,
    backgroundColor: Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(12.dp),
    ) {
        Text(content, style = MaterialTheme.typography.bodySmall)
    }
}

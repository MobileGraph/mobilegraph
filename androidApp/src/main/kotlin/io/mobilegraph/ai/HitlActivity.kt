package io.mobilegraph.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import io.mobilegraph.ai.viewmodel.HitlViewModel

class HitlActivity : ComponentActivity() {
    private val viewModel: HitlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.initializeSdk(applicationContext)
        setContent {
            MaterialTheme {
                HitlScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun HitlScreen(viewModel: HitlViewModel) {
    var topic by remember { mutableStateOf("The future of on-device AI") }
    var feedback by remember { mutableStateOf("") }
    val eventLog by viewModel.eventLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Human-in-the-Loop") })
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
            Text("Workflow Status: ${viewModel.uiState}", style = MaterialTheme.typography.titleMedium)

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Article Topic") },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.awaitingReview == null && !viewModel.isLoading,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.startWorkflow(topic) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading && topic.isNotBlank() && viewModel.awaitingReview == null,
            ) {
                Text("Start Writing Article")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.currentOutput.isNotEmpty()) {
                Text("Current Content:", fontWeight = FontWeight.Bold)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp),
                ) {
                    Text(viewModel.currentOutput)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // HITL Interaction Section
            if (viewModel.awaitingReview != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Human Review Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Please review the draft above. You can approve it to publish or reject it with feedback to rewrite.")

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = feedback,
                            onValueChange = { feedback = it },
                            label = { Text("Feedback (Optional for rejection)") },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { viewModel.submitReview(true, feedback) },
                                modifier = Modifier.weight(1f),
                                enabled = !viewModel.isLoading,
                            ) {
                                Text("Approve")
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(
                                onClick = { viewModel.submitReview(false, feedback) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = !viewModel.isLoading,
                            ) {
                                Text("Reject / Rewrite")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Event Log:", style = MaterialTheme.typography.titleSmall)
            eventLog.reversed().forEach { event ->
                Text("• $event", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

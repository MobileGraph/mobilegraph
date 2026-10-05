package io.mobilegraph.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.mobilegraph.ai.viewmodel.A2AViewModel

/**
 * Activity demonstrating A2A (Agent-to-Agent) Protocol Integration in MobileGraph
 * using the multi-skill A2A test server.
 */
class A2AActivity : ComponentActivity() {
    private val viewModel: A2AViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.initializeSdk(applicationContext)
        setContent {
            MaterialTheme {
                A2AScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun A2AScreen(viewModel: A2AViewModel) {
    var taskPrompt by remember {
        mutableStateOf("/math 10 multiply by 4")
    }
    val eventLog by viewModel.eventLog.collectAsState()
    val descriptor = viewModel.agentDescriptor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("A2A (Agent-to-Agent Protocol)") },
            )
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
            Text(
                "A2A Protocol Delegation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Connect to, discover, and delegate tasks to remote A2A-compatible multi-skill agents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 1: Agent Discovery Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. Agent Discovery & Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.remoteAgentUrl,
                        onValueChange = { viewModel.remoteAgentUrl = it },
                        label = { Text("Remote A2A Agent Endpoint URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.authToken,
                        onValueChange = { viewModel.authToken = it },
                        label = { Text("Bearer Token (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.discoverAgent(viewModel.remoteAgentUrl) },
                            enabled = !viewModel.isLoading && viewModel.remoteAgentUrl.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Discover Agent")
                        }

                        OutlinedButton(
                            onClick = { viewModel.setupDemoMockAgent() },
                            enabled = !viewModel.isLoading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Load Demo Agent")
                        }
                    }
                }
            }

            // --- Section 2: Discovered Agent Card Info ---
            if (descriptor != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            descriptor.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        descriptor.description?.let { desc ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Protocol: v${descriptor.protocolVersion ?: "0.3"}") },
                            )
                            if (descriptor.supportsStreaming) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("SSE Streaming") },
                                )
                            }
                            if (descriptor.requiresAuthentication) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Auth Required") },
                                )
                            }
                        }

                        if (descriptor.skills.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Advertised Skills:", style = MaterialTheme.typography.labelLarge)
                            descriptor.skills.forEach { skill ->
                                Text(
                                    "• ${skill.name}: ${skill.description ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 3: Task Execution Card with Skill Shortcuts ---
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "2. Execute Task via StateGraph Workflow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Try a Multi-Skill Prompt Preset:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SuggestionChip(
                            onClick = { taskPrompt = "/math 10 multiply by 4" },
                            label = { Text("Math: /math 10 multiply by 4") },
                        )
                        SuggestionChip(
                            onClick = { taskPrompt = "/greet Sarah" },
                            label = { Text("Greet: /greet Sarah") },
                        )
                        SuggestionChip(
                            onClick = { taskPrompt = "/echo Hello A2A Protocol!" },
                            label = { Text("Echo: /echo Hello A2A") },
                        )
                        SuggestionChip(
                            onClick = { taskPrompt = "/ai explain quantum computing in simple terms" },
                            label = { Text("AI: /ai quantum computing") },
                        )
                        SuggestionChip(
                            onClick = { taskPrompt = "/hitl transfer $500 to Alice" },
                            label = { Text("HITL: /hitl transfer $500 to Alice") },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskPrompt,
                        onValueChange = { taskPrompt = it },
                        label = { Text("Task Instructions for Remote A2A Agent") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use SSE Streaming Mode", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Uses StreamMessage (A2A v1.0) / message/stream (v0.3)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = viewModel.useStreaming,
                            onCheckedChange = { viewModel.useStreaming = it },
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.runA2AWorkflow(taskPrompt) },
                        enabled = !viewModel.isLoading && taskPrompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (viewModel.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(if (viewModel.isLoading) "Executing Workflow..." else "Run A2A Delegation Workflow")
                    }
                }
            }

            // --- Section 4: Results & Status ---
            Spacer(modifier = Modifier.height(16.dp))
            Text("Execution Status: ${viewModel.uiState}", style = MaterialTheme.typography.titleSmall)

            // --- A2A Human-in-the-Loop (HITL) Action Card ---
            val hitlReview = viewModel.awaitingReview
            if (hitlReview != null) {
                var userResponseText by remember { mutableStateOf("") }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🧑‍💻 A2A Human-in-the-Loop (HITL) Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "The remote A2A agent requires additional user input before proceeding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            viewModel.taskResult,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userResponseText,
                            onValueChange = { userResponseText = it },
                            label = { Text("Your Response / Instructions for Remote Agent") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.submitHitlResponse(userResponseText)
                                userResponseText = ""
                            },
                            enabled = !viewModel.isLoading && userResponseText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Submit Response to Remote Agent")
                        }
                    }
                }
            }

            if (viewModel.taskResult.isNotBlank() && hitlReview == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Agent Result / Artifacts:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            viewModel.taskResult,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // --- Section 5: MobileGraph Event Log ---
            Spacer(modifier = Modifier.height(24.dp))
            Text("MobileGraph Event Stream:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))

            if (eventLog.isEmpty()) {
                Text(
                    "No events yet. Click 'Discover Agent' to start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        eventLog.forEach { event ->
                            Text(
                                "• $event",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

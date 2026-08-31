package io.mobilegraph.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import io.mobilegraph.core.facade.MobileGraph

class MasterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MaterialTheme {
                MasterScreen(
                    onOptionSelected = { option ->
                        when (option) {
                            1 -> startActivity(Intent(this, MainActivity::class.java))
                            2 -> startActivity(Intent(this, RagActivity::class.java))
                            3 -> startActivity(Intent(this, AgentActivity::class.java))
                            4 -> startActivity(Intent(this, HitlActivity::class.java))
                            5 -> startActivity(Intent(this, ParallelAgentsActivity::class.java))
                            6 -> startActivity(Intent(this, ToolsAgentActivity::class.java))
                            7 -> startActivity(Intent(this, SubAgentActivity::class.java))
                            8 -> startActivity(Intent(this, MultiModelActivity::class.java))
                            9 -> startActivity(Intent(this, ModelRouterActivity::class.java))
                            10 -> startActivity(Intent(this, AgentRouterActivity::class.java))
                            11 -> startActivity(Intent(this, McpActivity::class.java))
                            12 -> startActivity(Intent(this, SkillActivity::class.java))
                        }
                    },
                )
            }
        }
    } // In your Activity, Fragment, or a custom Application cleanup

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            MobileGraph.terminate()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun MasterScreen(onOptionSelected: (Int) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MobileGraph SDK Tester") })
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
                "Select a functionality to test:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onOptionSelected(1) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("1. Basic Chat & Lifecycle ")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(2) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("2. RAG: Ingestion & Retrieval ")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(3) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("3. Simple Agent ")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(4) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("4. Human-in-the-Loop (HITL)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(5) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("5. Parallel Agents (Fan-out/in)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(6) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("6. Tool-Enabled Agents (Autonomous)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(7) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("7. Hierarchical Sub-Agents (Orchestration)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(8) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("8. Multi-Model & Vision (OpenAI, Gemini, Claude)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(9) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("9. Intelligent Model Router")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(10) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("10. Agent Brain Routing (Cost Optimization)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(11) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("11. MCP Integration (Remote Tools)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onOptionSelected(12) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("12. Skills System (Declarative AI)")
            }
        }
    }
}

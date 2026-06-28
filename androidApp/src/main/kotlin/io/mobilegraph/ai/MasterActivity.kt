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
                        }
                    },
                )
            }
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
                    .fillMaxSize(),
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
                Text("3. Agents & Tools ")
            }
        }
    }
}

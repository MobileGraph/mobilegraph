package io.mobilegraph.ai

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.mobilegraph.ai.viewmodel.RagIngestionViewModel
import io.mobilegraph.ai.viewmodel.RagRetrievalViewModel
import kotlinx.coroutines.flow.filterNotNull

class RagActivity : ComponentActivity() {
    private val ingestionViewModel: RagIngestionViewModel by viewModels()
    private val retrievalViewModel: RagRetrievalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ingestionViewModel.initialize(applicationContext)
        retrievalViewModel.initializeSdk(applicationContext)

        setContent {
            MaterialTheme {
                RagScreen(ingestionViewModel, retrievalViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun RagScreen(
    ingestionViewModel: RagIngestionViewModel,
    retrievalViewModel: RagRetrievalViewModel,
) {
    var searchQuery by remember { mutableStateOf("") }
    var targetDocId by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val ingestionLog by ingestionViewModel.eventLog.collectAsState()
    val retrievalLog by retrievalViewModel.eventLog.collectAsState()
    val availableDocIds by retrievalViewModel.availableDocIds.collectAsState()
    val ingestionSuccess by ingestionViewModel.ingestionSuccess.collectAsState()

    // Map of predefined queries for each test document
    val predefinedQueries =
        mapOf(
            "txt-doc" to "Where is Headquarter of ACME Robotics?",
            "md-doc" to "Tell me about coding guidelines.",
            "html-doc" to "Explain me about RoboLift X100.",
            "json-doc" to "Give me details of FM300.",
            "csv-doc" to "Who is Bob Smith?",
            "pdf-doc" to "What is Error E205?",
            "web-doc" to "What is Kotlin Multiplatform?",
            "mobile-adk" to "Explain Agent Development Kit.",
        )

    LaunchedEffect(ingestionSuccess) {
        ingestionSuccess?.let { id ->
            retrievalViewModel.refreshDocIds()
            snackbarHostState.showSnackbar("Successfully ingested $id")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(" RAG") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            Text("Status: ${retrievalViewModel.uiState}", style = MaterialTheme.typography.titleMedium)

            if (ingestionViewModel.isLoading || retrievalViewModel.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Ingestion Tests", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    ingestionViewModel.ingestText()
                }, modifier = Modifier.weight(1f)) { Text("Text") }
                Button(onClick = {
                    ingestionViewModel.ingestMarkdown()
                }, modifier = Modifier.weight(1f)) { Text("MD") }
                Button(onClick = {
                    ingestionViewModel.ingestHtml()
                }, modifier = Modifier.weight(1f)) { Text("HTML") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    ingestionViewModel.ingestJson()
                }, modifier = Modifier.weight(1f)) { Text("JSON") }
                Button(onClick = {
                    ingestionViewModel.ingestCsv()
                }, modifier = Modifier.weight(1f)) { Text("CSV") }
                Button(onClick = {
                    ingestionViewModel.ingestPdf("sample.pdf")
                }, modifier = Modifier.weight(1f)) { Text("PDF") }
            }
            Button(
                onClick = {
                    ingestionViewModel.ingestWebsite("https://kotlinlang.org")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scrape Website (kotlinlang.org)")
            }

            // Button to refresh doc IDs manually or triggered by ingestion success (in a real app we'd observe a flow)
            Button(
                onClick = { retrievalViewModel.refreshDocIds() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Refresh Knowledge Inventory")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Retrieval Tests", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = targetDocId,
                    onValueChange = {
                        targetDocId = it
                        predefinedQueries[it]?.let { q -> searchQuery = q }
                    },
                    label = { Text("Filter by Document ID (Optional)") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )

                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All Documents (No Filter)") },
                        onClick = {
                            targetDocId = ""
                            searchQuery = "General question about all docs..."
                            isExpanded = false
                        },
                    )
                    availableDocIds.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(id) },
                            onClick = {
                                targetDocId = id
                                searchQuery = predefinedQueries[id] ?: "Search in $id..."
                                isExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Query") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentDocIdSelection = if (targetDocId.isBlank()) null else targetDocId
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    retrievalViewModel.searchWithVector(searchQuery, currentDocIdSelection)
                }, modifier = Modifier.weight(1f)) { Text("Vector") }
                Button(
                    onClick = { retrievalViewModel.searchWithBM25(searchQuery, currentDocIdSelection) },
                    modifier = Modifier.weight(1f),
                ) { Text("BM25") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    retrievalViewModel.searchWithHybrid(searchQuery, currentDocIdSelection)
                }, modifier = Modifier.weight(1f)) { Text("Hybrid") }
                Button(onClick = {
                    retrievalViewModel.searchWithMultiQuery(searchQuery, currentDocIdSelection)
                }, modifier = Modifier.weight(1f)) { Text("MultiQuery") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (retrievalViewModel.currentOutput.isNotEmpty()) {
                Text("Results:", style = MaterialTheme.typography.titleSmall)
                Text(retrievalViewModel.currentOutput, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Event Log:", style = MaterialTheme.typography.titleSmall)
            (ingestionLog + retrievalLog).reversed().forEach { event ->
                Text("• $event", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

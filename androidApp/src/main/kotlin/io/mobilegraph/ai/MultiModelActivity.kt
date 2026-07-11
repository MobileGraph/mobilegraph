package io.mobilegraph.ai

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.mobilegraph.ai.viewmodel.MultiModelViewModel

class MultiModelActivity : ComponentActivity() {
    private val viewModel: MultiModelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.initializeSdk(applicationContext)
        setContent {
            MaterialTheme {
                MultiModelScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ktlint:standard:function-naming")
fun MultiModelScreen(viewModel: MultiModelViewModel) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("What do you see in this image?") }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    viewModel.selectedImageBytes = stream.readBytes()
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Multi-Model & Vision Test") })
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
            Text("Select Provider:", style = MaterialTheme.typography.titleSmall)

            LazyRow(modifier = Modifier.fillMaxWidth()) {
                items(viewModel.providers.toList()) { (name, modelId) ->
                    FilterChip(
                        selected = viewModel.selectedProvider == modelId,
                        onClick = { viewModel.selectedProvider = modelId },
                        label = {
                            Text(
                                name,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Text-Only Quick Tests:", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth()) {
                val tests =
                    mapOf(
                        "Summarize" to "Summarize the history of AI in 2 sentences.",
                        "Translate" to "Translate 'The weather is beautiful' to French.",
                        "Poem" to "Write a 4-line poem about a mobile robot.",
                    )
                tests.forEach { (label, prompt) ->
                    Button(
                        onClick = {
                            query = prompt
                            viewModel.selectedImageBytes = null
                            viewModel.runQuery(prompt)
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Query") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Vision Input (Gallery or URL):", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("+ Pick from Gallery")
                }

                if (viewModel.selectedImageBytes != null) {
                    Button(
                        onClick = { viewModel.selectedImageBytes = null },
                        modifier = Modifier.padding(start = 8.dp),
                        colors =
                            androidx.compose.material3.ButtonDefaults
                                .buttonColors(containerColor = Color.Red),
                    ) {
                        Text("Clear")
                    }
                }
            }

            if (viewModel.selectedImageBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(viewModel.selectedImageBytes, 0, viewModel.selectedImageBytes!!.size)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier =
                            Modifier
                                .padding(top = 8.dp)
                                .size(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.runQuery(query) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading,
            ) {
                Text("Invoke ${viewModel.selectedProvider}")
            }

            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (viewModel.responseText.isNotEmpty()) {
                Text("Response:", fontWeight = FontWeight.Bold)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF3F3F3))
                            .padding(12.dp),
                ) {
                    Text(viewModel.responseText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Status: ${viewModel.uiState}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

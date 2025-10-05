package com.ainotebuddy.app.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import kotlinx.coroutines.launch

/**
 * Simple test activity to verify AI integration works
 */
class AITestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AINoteBuddyTheme {
                AITestScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITestScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val aiService = remember { NoteBuddyAIService(context) }
    val scope = rememberCoroutineScope()
    
    var testInput by remember { mutableStateOf("This is a test note about machine learning and AI.") }
    var testResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI Integration Test") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Test AI Integration",
                style = MaterialTheme.typography.headlineSmall
            )
            
            OutlinedTextField(
                value = testInput,
                onValueChange = { testInput = it },
                label = { Text("Test Input") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val response = aiService.processAIRequest(
                                    "enhance",
                                    testInput,
                                    "Test Note"
                                )
                                testResult = "Success: ${response.content}"
                            } catch (e: Exception) {
                                testResult = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Test Enhance")
                    }
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val response = aiService.processAIRequest(
                                    "summarize",
                                    testInput,
                                    "Test Note"
                                )
                                testResult = "Success: ${response.content}"
                            } catch (e: Exception) {
                                testResult = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Test Summarize")
                    }
                }
            }
            
            if (testResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Result:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testResult,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Status Info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI Configuration:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Provider: ${preferencesManager.getAIProvider()}")
                    Text("Has Valid API Key: ${preferencesManager.hasValidApiKey()}")
                    Text("Model: ${preferencesManager.getAIModel()}")
                }
            }
        }
    }
}
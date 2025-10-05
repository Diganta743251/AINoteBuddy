package com.ainotebuddy.app.features

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.testing.APIKeyTestingUtility
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun APIKeySetupScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val testingUtility = remember { APIKeyTestingUtility(context, preferencesManager) }
    
    var openAIKey by remember { mutableStateOf(preferencesManager.getOpenAIKey() ?: "") }
    var geminiKey by remember { mutableStateOf(preferencesManager.getGeminiKey() ?: "") }
    var claudeKey by remember { mutableStateOf(preferencesManager.getClaudeKey() ?: "") }
    
    var showOpenAIKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }
    
    var testResults by remember { mutableStateOf<Map<String, APIKeyTestingUtility.APIKeyTestResult>>(emptyMap()) }
    var isTestingKeys by remember { mutableStateOf(false) }
    var showCostInfo by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCostInfo = !showCostInfo }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Cost Information",
                            tint = if (showCostInfo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introduction Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Security",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Your API Keys, Your Control",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "AINoteBuddy uses your own API keys, giving you complete control over costs and usage. Keys are stored securely on your device and never shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Cost Information (Expandable)
            if (showCostInfo) {
                CostInformationCard(testingUtility)
            }
            
            // OpenAI API Key Section
            APIKeySection(
                title = "OpenAI API Key",
                description = "For GPT-3.5 and GPT-4 models",
                value = openAIKey,
                onValueChange = { openAIKey = it },
                showKey = showOpenAIKey,
                onToggleVisibility = { showOpenAIKey = !showOpenAIKey },
                testResult = testResults["OpenAI"],
                setupInstructions = testingUtility.getAPIKeySetupInstructions()["OpenAI"] ?: "",
                isOptional = true
            )
            
            // Gemini API Key Section
            APIKeySection(
                title = "Gemini API Key",
                description = "For Google's Gemini models (Free tier available)",
                value = geminiKey,
                onValueChange = { geminiKey = it },
                showKey = showGeminiKey,
                onToggleVisibility = { showGeminiKey = !showGeminiKey },
                testResult = testResults["Gemini"],
                setupInstructions = testingUtility.getAPIKeySetupInstructions()["Gemini"] ?: "",
                isOptional = true,
                isRecommended = true
            )
            
            // Claude API Key Section
            APIKeySection(
                title = "Claude API Key",
                description = "For Anthropic's Claude models",
                value = claudeKey,
                onValueChange = { claudeKey = it },
                showKey = showClaudeKey,
                onToggleVisibility = { showClaudeKey = !showClaudeKey },
                testResult = testResults["Claude"],
                setupInstructions = testingUtility.getAPIKeySetupInstructions()["Claude"] ?: "",
                isOptional = true
            )
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTestingKeys = true
                            val results = mutableMapOf<String, APIKeyTestingUtility.APIKeyTestResult>()
                            
                            if (openAIKey.isNotBlank()) {
                                results["OpenAI"] = testingUtility.testOpenAIKey(openAIKey)
                            }
                            if (geminiKey.isNotBlank()) {
                                results["Gemini"] = testingUtility.testGeminiKey(geminiKey)
                            }
                            if (claudeKey.isNotBlank()) {
                                results["Claude"] = testingUtility.testClaudeKey(claudeKey)
                            }
                            
                            testResults = results
                            isTestingKeys = false
                        }
                    },
                    enabled = !isTestingKeys && (openAIKey.isNotBlank() || geminiKey.isNotBlank() || claudeKey.isNotBlank()),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingKeys) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Keys")
                }
                
                Button(
                    onClick = {
                        // Save keys
                        if (openAIKey.isNotBlank()) preferencesManager.setOpenAIKey(openAIKey)
                        if (geminiKey.isNotBlank()) preferencesManager.setGeminiKey(geminiKey)
                        if (claudeKey.isNotBlank()) preferencesManager.setClaudeKey(claudeKey)
                        
                        onComplete()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save & Continue")
                }
            }
            
            // Usage Recommendations
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Recommendations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    testingUtility.getAPIKeyRecommendations().forEach { (category, recommendation) ->
                        Text(
                            text = "• $category: $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Skip Option
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now (AI features will be limited)")
            }
        }
    }
}

@Composable
private fun APIKeySection(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    showKey: Boolean,
    onToggleVisibility: () -> Unit,
    testResult: APIKeyTestingUtility.APIKeyTestResult?,
    setupInstructions: String,
    isOptional: Boolean = true,
    isRecommended: Boolean = false
) {
    var showInstructions by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                testResult?.isValid == true -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                testResult?.isValid == false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isOptional) {
                            Badge(
                                containerColor = if (isRecommended) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline
                            ) {
                                Text(
                                    text = if (isRecommended) "RECOMMENDED" else "OPTIONAL",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { showInstructions = !showInstructions }) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = "Setup Instructions",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Setup Instructions (Expandable)
            if (showInstructions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = setupInstructions,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // API Key Input
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("API Key") },
                placeholder = { Text("Enter your $title...") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide key" else "Show key"
                        )
                    }
                },
                isError = testResult?.isValid == false,
                supportingText = {
                    testResult?.let { result ->
                        Text(
                            text = result.errorMessage ?: result.usageInfo ?: "",
                            color = if (result.isValid) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            
            // Test Result
            testResult?.let { result ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.isValid) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (result.isValid) "Valid API key" else "Invalid API key",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.isValid) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    result.costEstimate?.let { cost ->
                        Text(
                            text = " • $cost",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CostInformationCard(testingUtility: APIKeyTestingUtility) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💰 Cost Estimates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            testingUtility.getCostEstimations().forEach { (usage, cost) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = usage,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = cost,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            
            HorizontalDivider()
            
            Text(
                text = "💡 All costs are paid directly by you to the AI service providers. AINoteBuddy never charges for AI usage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.ainotebuddy.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.AIProvider
import com.ainotebuddy.app.data.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var selectedProvider by remember { mutableStateOf(preferencesManager.getAIProvider()) }
    var openAIKey by remember { mutableStateOf(preferencesManager.getOpenAIKey() ?: "") }
    var geminiKey by remember { mutableStateOf(preferencesManager.getGeminiKey() ?: "") }
    var claudeKey by remember { mutableStateOf(preferencesManager.getClaudeKey() ?: "") }
    
    var showOpenAIKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
            // Provider Selection
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    AIProvider.values().forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = {
                                    selectedProvider = provider
                                    preferencesManager.setAIProvider(provider)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (provider) {
                                    AIProvider.OPENAI -> "OpenAI (GPT)"
                                    AIProvider.GEMINI -> "Google Gemini"
                                    AIProvider.CLAUDE -> "Anthropic Claude"
                                    AIProvider.OFFLINE -> "Offline Mode"
                                }
                            )
                        }
                    }
                }
            }
            
            // API Keys Section
            if (selectedProvider != AIProvider.OFFLINE) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "API Keys",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // OpenAI Key
                        if (selectedProvider == AIProvider.OPENAI) {
                            APIKeyField(
                                label = "OpenAI API Key",
                                value = openAIKey,
                                onValueChange = { 
                                    openAIKey = it
                                    preferencesManager.setOpenAIKey(it)
                                },
                                isVisible = showOpenAIKey,
                                onVisibilityToggle = { showOpenAIKey = !showOpenAIKey },
                                instructions = "Get your API key from: https://platform.openai.com/api-keys"
                            )
                        }
                        
                        // Gemini Key
                        if (selectedProvider == AIProvider.GEMINI) {
                            APIKeyField(
                                label = "Gemini API Key",
                                value = geminiKey,
                                onValueChange = { 
                                    geminiKey = it
                                    preferencesManager.setGeminiKey(it)
                                },
                                isVisible = showGeminiKey,
                                onVisibilityToggle = { showGeminiKey = !showGeminiKey },
                                instructions = "Get your API key from: https://makersuite.google.com/app/apikey"
                            )
                        }
                        
                        // Claude Key
                        if (selectedProvider == AIProvider.CLAUDE) {
                            APIKeyField(
                                label = "Claude API Key",
                                value = claudeKey,
                                onValueChange = { 
                                    claudeKey = it
                                    preferencesManager.setClaudeKey(it)
                                },
                                isVisible = showClaudeKey,
                                onVisibilityToggle = { showClaudeKey = !showClaudeKey },
                                instructions = "Get your API key from: https://console.anthropic.com/"
                            )
                        }
                    }
                }
            }
            
            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to get API Keys",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "• OpenAI: Sign up at openai.com, go to API section, create new key",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Gemini: Visit Google AI Studio, create project, generate API key",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Claude: Register at Anthropic Console, create API key in settings",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Offline: Uses local processing, no internet required",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun APIKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    instructions: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isVisible) "Hide" else "Show"
                    )
                }
            },
            singleLine = true
        )
        
        Text(
            text = instructions,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
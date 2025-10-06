package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.ainotebuddy.app.BuildConfig
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.AIProvider
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ui.components.PreferenceItem
import com.ainotebuddy.app.ui.components.PreferenceSubtitle
import com.ainotebuddy.app.ui.components.PreferenceTitle
import com.ainotebuddy.app.MainActivity
import com.ainotebuddy.app.ui.theme.ModernColors
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    onBack: () -> Unit,
    onVoiceCommandSettingsClick: () -> Unit = {},
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State for the selected provider
    var selectedProvider by remember { mutableStateOf(AIProvider.OPENAI) }
    
    // State for API keys
    var openAIKey by remember { mutableStateOf("") }
    var geminiKey by remember { mutableStateOf("") }
    var claudeKey by remember { mutableStateOf("") }
    
    // Load keys from preferences when preferencesManager changes
    LaunchedEffect(preferencesManager) {
        openAIKey = preferencesManager.getOpenAIKey() ?: ""
        geminiKey = preferencesManager.getGeminiKey() ?: ""
        claudeKey = preferencesManager.getClaudeKey() ?: ""
    }
    
    // State for showing API key dialogs
    var showOpenAIDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var showClaudeDialog by remember { mutableStateOf(false) }
    
    // State for showing validation messages
    var showValidationError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    
    // Show a snackbar for validation messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Voice Command Settings
            PreferenceTitle(text = "Voice Commands")
            PreferenceItem(
                title = "Voice Command Settings",
                subtitle = "Configure voice commands and privacy",
                icon = Icons.Default.KeyboardVoice,
                onClick = onVoiceCommandSettingsClick
            )
            
            // AI Provider Selection
            PreferenceTitle(text = "AI Provider")
            PreferenceSubtitle(text = "Choose your preferred AI service")
            
            AIProvider.values().forEach { provider ->
                if (provider != AIProvider.OFFLINE) { // Skip offline mode in the list
                    PreferenceItem(
                        title = provider.displayName,
                        subtitle = provider.description,
                        icon = when (provider) {
                            AIProvider.OPENAI -> Icons.Default.SmartToy
                            AIProvider.GEMINI -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Info
                        },
                        onClick = {
                            selectedProvider = provider
                            preferencesManager.setAIProvider(provider)
                        }
                    )
                }
            }
            
            // End of AI provider selection block
            // API Key Management
            PreferenceTitle(text = "API Keys")
            PreferenceSubtitle(text = "Manage your API keys for AI services")
            
            // OpenAI API Key
            PreferenceItem(
                title = "OpenAI API Key",
                subtitle = if (openAIKey.isNotEmpty()) "••••••••${openAIKey.takeLast(4)}" 
                         else "Not configured",
                icon = Icons.Default.VpnKey,
                onClick = { showOpenAIDialog = true }
            )
            
            // Gemini API Key
            PreferenceItem(
                title = "Google Gemini API Key",
                subtitle = if (geminiKey.isNotEmpty()) "••••••••${geminiKey.takeLast(4)}" 
                         else "Not configured",
                icon = Icons.Default.VpnKey,
                onClick = { showGeminiDialog = true }
            )
            
            // Claude API Key
            PreferenceItem(
                title = "Anthropic Claude API Key",
                subtitle = if (claudeKey.isNotEmpty()) "••••••••${claudeKey.takeLast(4)}" 
                         else "Not configured",
                icon = Icons.Default.VpnKey,
                onClick = { showClaudeDialog = true }
            )
            
            // AI Model Selection
            val models = when (selectedProvider) {
                AIProvider.OPENAI -> listOf("gpt-3.5-turbo", "gpt-4", "gpt-4-turbo")
                AIProvider.GEMINI -> listOf("gemini-pro", "gemini-1.5-pro")
                AIProvider.CLAUDE -> listOf("claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307")
                AIProvider.OFFLINE -> listOf("offline")
            }
            
            var selectedModel by remember { mutableStateOf(preferencesManager.getAIModel()) }
            
            PreferenceTitle(text = "Model")
            PreferenceSubtitle(text = "Select the AI model to use")
            
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(horizontal = 16.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                selectedModel = model
                                preferencesManager.setAIModel(model)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            
            // AI Settings
            PreferenceTitle(text = "AI Behavior")
            PreferenceSubtitle(text = "Customize how AI interacts with your notes")
            
            var temperature by remember { 
                mutableFloatStateOf(preferencesManager.getAITemperature()) 
            }
            
            PreferenceItem(
                title = "Creativity (${(temperature * 10).toInt()}/10)",
                subtitle = "Higher values make the AI more creative but less focused",
                icon = Icons.Default.AutoFixHigh
            )
            
            Slider(
                value = temperature,
                onValueChange = { 
                    temperature = it
                    preferencesManager.setAITemperature(it)
                },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Clear API Keys (for testing/debugging)
            if (BuildConfig.DEBUG) {
                PreferenceItem(
                    title = "Clear API Keys",
                    onClick = { 
                        preferencesManager.clearAllApiKeys()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("API keys cleared")
                        }
                    }
                )
            }
        }
        
        // API Key Management
        PreferenceTitle(text = "API Keys")
        PreferenceSubtitle(text = "Manage your API keys for AI services")
        
        // OpenAI API Key
        PreferenceItem(
            title = "OpenAI API Key",
            summary = if (openAIKey.isNotBlank()) "Configured" else "Not configured",
            onClick = { showOpenAIDialog = true }
        )
    }
    
    // API Key Dialogs
    if (showOpenAIDialog) {
        ApiKeyDialog(
            title = "OpenAI API Key",
            currentKey = openAIKey,
            onDismiss = { showOpenAIDialog = false },
            onSave = { key ->
                if (preferencesManager.validateApiKey(AIProvider.OPENAI, key)) {
                    preferencesManager.setOpenAIKey(key)
                    openAIKey = key
                    showOpenAIDialog = false
                    
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("OpenAI API key saved")
                    }
                } else {
                    validationMessage = "Invalid OpenAI API key format. It should start with 'sk-' and be at least 30 characters long."
                    showValidationError = true
                }
            }
        )
    }
    
    if (showGeminiDialog) {
        ApiKeyDialog(
            title = "Google Gemini API Key",
            currentKey = geminiKey,
            onDismiss = { showGeminiDialog = false },
            onSave = { key ->
                if (preferencesManager.validateApiKey(AIProvider.GEMINI, key)) {
                    preferencesManager.setGeminiKey(key)
                    geminiKey = key
                    showGeminiDialog = false
                    
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Gemini API key saved")
                    }
                } else {
                    validationMessage = "Invalid Gemini API key format. It should be at least 20 characters long."
                    showValidationError = true
                }
            }
        )
    }
    
    if (showClaudeDialog) {
        ApiKeyDialog(
            title = "Anthropic Claude API Key",
            currentKey = claudeKey,
            onDismiss = { showClaudeDialog = false },
            onSave = { key ->
                if (preferencesManager.validateApiKey(AIProvider.CLAUDE, key)) {
                    preferencesManager.setClaudeKey(key)
                    claudeKey = key
                    showClaudeDialog = false
                    
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Claude API key saved")
                    }
                } else {
                    validationMessage = "Invalid Claude API key format. It should start with 'sk-ant-' and be at least 40 characters long."
                    showValidationError = true
                }
            }
        )
    }
    
    // Show validation error if needed
    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Invalid API Key") },
            text = { Text(validationMessage) },
            confirmButton = {
                TextButton(onClick = { showValidationError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ApiKeyDialog(
    title: String,
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter your API key for $title")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    visualTransformation = if (apiKey.isNotEmpty()) 
                        PasswordVisualTransformation() 
                    else 
                        VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Your API key is stored securely on your device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PreferenceTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun PreferenceSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

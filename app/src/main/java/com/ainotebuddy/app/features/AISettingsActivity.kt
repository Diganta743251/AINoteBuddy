package com.ainotebuddy.app.features

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.*
import com.ainotebuddy.app.ai.AIApiClient
import com.ainotebuddy.app.viewmodel.AISettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AISettingsActivity : ComponentActivity() {
    
    private val viewModel: AISettingsViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AINoteBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AISettingsScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() },
                        onOpenApiKeySettings = {
                            val intent = Intent(this@AISettingsActivity, ApiKeySettingsActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    viewModel: AISettingsViewModel,
    onBackPressed: () -> Unit,
    onOpenApiKeySettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.testConnection() },
                        enabled = !uiState.isTestingConnection
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Test Connection")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Provider Selection
            item {
                AIProviderSection(
                    selectedProvider = uiState.selectedProvider,
                    onProviderSelected = viewModel::setAIProvider
                )
            }
            
            // API Key Configuration
            item {
                APIKeySection(
                    provider = uiState.selectedProvider,
                    apiKeys = uiState.apiKeys,
                    onApiKeyChanged = viewModel::setApiKey,
                    isTestingConnection = uiState.isTestingConnection,
                    connectionStatus = uiState.connectionStatus,
                    onOpenApiKeySettings = onOpenApiKeySettings
                )
            }
            
            // AI Model Settings
            if (uiState.selectedProvider != AIProvider.OFFLINE) {
                item {
                    AIModelSection(
                        provider = uiState.selectedProvider,
                        selectedModel = uiState.selectedModel,
                        availableModels = uiState.availableModels,
                        onModelSelected = viewModel::setModel
                    )
                }
                
                item {
                    AIParametersSection(
                        temperature = uiState.temperature,
                        maxTokens = uiState.maxTokens,
                        onTemperatureChanged = viewModel::setTemperature,
                        onMaxTokensChanged = viewModel::setMaxTokens
                    )
                }
            }
            
            // Feature Toggles
            item {
                FeatureTogglesSection(
                    autoEnhanceEnabled = uiState.autoEnhanceEnabled,
                    voiceProcessingEnabled = uiState.voiceProcessingEnabled,
                    smartCategorizationEnabled = uiState.smartCategorizationEnabled,
                    onAutoEnhanceToggled = { _ -> viewModel.toggleAutoEnhance() },
                    onVoiceProcessingToggled = { _ -> viewModel.toggleVoiceProcessing() },
                    onSmartCategorizationToggled = { _ -> viewModel.toggleSmartCategorization() }
                )
            }
            
            // Usage Statistics
            item {
                UsageStatsSection(
                    usageCount = uiState.usageCount,
                    lastUsed = uiState.lastUsed
                )
            }
            
            // Reset Section
            item {
                ResetSection(
                    onResetApiKeys = viewModel::resetApiKeys,
                    onResetSettings = viewModel::resetAllSettings
                )
            }
        }
    }
    
    // Connection test result dialog
    if (uiState.showConnectionResult) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionResult() },
            title = { Text("Connection Test") },
            text = { 
                Text(
                    text = if (uiState.connectionStatus == ConnectionStatus.SUCCESS) {
                        "✅ Connection successful! Your API key is working correctly."
                    } else {
                        "❌ Connection failed. Please check your API key and internet connection."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissConnectionResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AIProviderSection(
    selectedProvider: AIProvider,
    onProviderSelected: (AIProvider) -> Unit
) {
    SettingsCard(
        title = "AI Provider",
        icon = Icons.Default.Psychology
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AIProvider.values().forEach { provider ->
                ProviderCard(
                    provider = provider,
                    isSelected = provider == selectedProvider,
                    onSelected = { onProviderSelected(provider) }
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AIProvider,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onSelected
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = provider.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun APIKeySection(
    provider: AIProvider,
    apiKeys: Map<AIProvider, String>,
    onApiKeyChanged: (AIProvider, String) -> Unit,
    isTestingConnection: Boolean,
    connectionStatus: ConnectionStatus?,
    onOpenApiKeySettings: () -> Unit
) {
    if (provider == AIProvider.OFFLINE) return
    
    SettingsCard(
        title = "API Key Configuration",
        icon = Icons.Default.Key
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Enter your ${provider.displayName} API key to enable AI features:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            var showApiKey by remember { mutableStateOf(false) }
            val currentKey = apiKeys[provider] ?: ""
            
            OutlinedTextField(
                value = currentKey,
                onValueChange = { onApiKeyChanged(provider, it) },
                label = { Text("${provider.displayName} API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                        )
                    }
                },
                supportingText = {
                    when (connectionStatus) {
                        ConnectionStatus.SUCCESS -> {
                            Text(
                                text = "✅ API key is valid",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        ConnectionStatus.FAILED -> {
                            Text(
                                text = "❌ API key is invalid or connection failed",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        null -> {
                            Text(getApiKeyInstructions(provider))
                        }
                    }
                },
                isError = connectionStatus == ConnectionStatus.FAILED
            )
            
            // API Key Instructions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "How to get your API key:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = getApiKeyInstructions(provider),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Advanced API Key Settings Button
            OutlinedButton(
                onClick = onOpenApiKeySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced API Key Settings")
            }
        }
    }
}

@Composable
private fun AIModelSection(
    provider: AIProvider,
    selectedModel: String,
    availableModels: List<String>,
    onModelSelected: (String) -> Unit
) {
    SettingsCard(
        title = "AI Model",
        icon = Icons.Default.Memory
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Select the AI model to use:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            availableModels.forEach { model ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = model == selectedModel,
                        onClick = { onModelSelected(model) }
                    )
                    Text(
                        text = model,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIParametersSection(
    temperature: Float,
    maxTokens: Int,
    onTemperatureChanged: (Float) -> Unit,
    onMaxTokensChanged: (Int) -> Unit
) {
    SettingsCard(
        title = "AI Parameters",
        icon = Icons.Default.Tune
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Temperature Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Creativity (Temperature)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = String.format(java.util.Locale.ROOT, "%.1f", temperature),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChanged,
                    valueRange = 0.0f..1.0f,
                    steps = 9
                )
                Text(
                    text = "Lower values = more focused, Higher values = more creative",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Max Tokens Input
            Column {
                Text(
                    text = "Maximum Response Length",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = maxTokens.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { tokens ->
                            if (tokens in 100..4000) {
                                onMaxTokensChanged(tokens)
                            }
                        }
                    },
                    label = { Text("Max Tokens") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Range: 100-4000 tokens") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeatureTogglesSection(
    autoEnhanceEnabled: Boolean,
    voiceProcessingEnabled: Boolean,
    smartCategorizationEnabled: Boolean,
    onAutoEnhanceToggled: (Boolean) -> Unit,
    onVoiceProcessingToggled: (Boolean) -> Unit,
    onSmartCategorizationToggled: (Boolean) -> Unit
) {
    SettingsCard(
        title = "AI Features",
        icon = Icons.Default.AutoAwesome
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureToggle(
                title = "Auto-enhance Notes",
                description = "Automatically improve grammar and clarity when saving",
                checked = autoEnhanceEnabled,
                onCheckedChange = onAutoEnhanceToggled
            )
            
            FeatureToggle(
                title = "Voice Processing",
                description = "Clean up voice transcriptions with AI",
                checked = voiceProcessingEnabled,
                onCheckedChange = onVoiceProcessingToggled
            )
            
            FeatureToggle(
                title = "Smart Categorization",
                description = "Automatically suggest categories and tags",
                checked = smartCategorizationEnabled,
                onCheckedChange = onSmartCategorizationToggled
            )
        }
    }
}

@Composable
private fun FeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun UsageStatsSection(
    usageCount: Int,
    lastUsed: String
) {
    SettingsCard(
        title = "Usage Statistics",
        icon = Icons.Default.Analytics
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem(
                label = "Total AI Requests",
                value = usageCount.toString()
            )
            StatItem(
                label = "Last Used",
                value = lastUsed
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ResetSection(
    onResetApiKeys: () -> Unit,
    onResetSettings: () -> Unit
) {
    SettingsCard(
        title = "Reset Options",
        icon = Icons.Default.RestartAlt
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onResetApiKeys,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All API Keys")
            }
            
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

private fun getApiKeyInstructions(provider: AIProvider): String {
    return when (provider) {
        AIProvider.OPENAI -> "Visit platform.openai.com → API Keys → Create new secret key"
        AIProvider.GEMINI -> "Visit makersuite.google.com → Get API Key → Create API key"
        AIProvider.CLAUDE -> "Visit console.anthropic.com → API Keys → Create key"
        AIProvider.OFFLINE -> ""
    }
}
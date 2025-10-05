package com.ainotebuddy.app.features

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.AIProvider
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.ui.theme.GlassCard

class ApiKeySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AINoteBuddyTheme {
                ApiKeySettingsScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var selectedProvider by remember { mutableStateOf(preferencesManager.getAIProvider()) }
    var openaiKey by remember { mutableStateOf(preferencesManager.getOpenAIKey() ?: "") }
    var geminiKey by remember { mutableStateOf(preferencesManager.getGeminiKey() ?: "") }
    var claudeKey by remember { mutableStateOf(preferencesManager.getClaudeKey() ?: "") }
    
    var showOpenAIKey by remember { mutableStateOf(false) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Save all keys
                            preferencesManager.setOpenAIKey(openaiKey)
                            preferencesManager.setGeminiKey(geminiKey)
                            preferencesManager.setClaudeKey(claudeKey)
                            preferencesManager.setAIProvider(selectedProvider)
                            
                            Toast.makeText(context, "API keys saved successfully", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                        }
                    ) {
                        Icon(Icons.Default.Save, "Save")
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
            // Instructions Card
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "AI Provider Setup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configure your AI provider and API keys to enable AI features like note enhancement, summarization, and smart categorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Provider Selection
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Select AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AIProvider.values().forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = when (provider) {
                                        AIProvider.OPENAI -> "GPT-4, GPT-3.5 Turbo"
                                        AIProvider.GEMINI -> "Gemini Pro, Gemini Pro Vision"
                                        AIProvider.CLAUDE -> "Claude 3 Sonnet, Claude 3 Haiku"
                                        AIProvider.OFFLINE -> "Local processing (limited features)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            // OpenAI Configuration
            if (selectedProvider == AIProvider.OPENAI) {
                ApiKeyCard(
                    title = "OpenAI API Key",
                    description = "Get your API key from platform.openai.com",
                    value = openaiKey,
                    onValueChange = { openaiKey = it },
                    isVisible = showOpenAIKey,
                    onVisibilityToggle = { showOpenAIKey = !showOpenAIKey },
                    instructionUrl = "https://platform.openai.com/api-keys"
                )
            }
            
            // Gemini Configuration
            if (selectedProvider == AIProvider.GEMINI) {
                ApiKeyCard(
                    title = "Google AI Studio API Key",
                    description = "Get your API key from aistudio.google.com",
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    isVisible = showGeminiKey,
                    onVisibilityToggle = { showGeminiKey = !showGeminiKey },
                    instructionUrl = "https://aistudio.google.com/app/apikey"
                )
            }
            
            // Claude Configuration
            if (selectedProvider == AIProvider.CLAUDE) {
                ApiKeyCard(
                    title = "Anthropic API Key",
                    description = "Get your API key from console.anthropic.com",
                    value = claudeKey,
                    onValueChange = { claudeKey = it },
                    isVisible = showClaudeKey,
                    onVisibilityToggle = { showClaudeKey = !showClaudeKey },
                    instructionUrl = "https://console.anthropic.com/account/keys"
                )
            }
            
            // Offline Mode Info
            if (selectedProvider == AIProvider.OFFLINE) {
                GlassCard {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Offline mode provides basic text processing without requiring an internet connection or API keys. Features include basic formatting, spell check, and simple text analysis.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Security Notice
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Security Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your API keys are encrypted and stored securely on your device. They are never shared with third parties and are only used to communicate directly with your chosen AI provider.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyCard(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    instructionUrl: String
) {
    GlassCard {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("API Key") },
                placeholder = { Text("Enter your API key") },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(
                            if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide API key" else "Show API key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Need help? Visit $instructionUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
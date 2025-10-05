package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.Context
import com.ainotebuddy.app.R
import com.ainotebuddy.app.preferences.isVoiceCommandsEnabled
import com.ainotebuddy.app.preferences.isWakeWordEnabled
import com.ainotebuddy.app.preferences.isAudioFeedbackEnabled
import com.ainotebuddy.app.preferences.isVibrateOnCommand
import com.ainotebuddy.app.preferences.isAutoPunctuationEnabled
import com.ainotebuddy.app.preferences.isPrivacyModeEnabled
import com.ainotebuddy.app.preferences.voiceCommandLanguage
import com.ainotebuddy.app.ui.components.PreferenceItem
import com.ainotebuddy.app.ui.components.PreferenceSwitch
import com.ainotebuddy.app.ui.components.SectionHeader
import com.ainotebuddy.app.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val prefs = remember { context.getSharedPreferences("ai_notebuddy_prefs", Context.MODE_PRIVATE) }
    
    // Voice command preferences (explicit types to avoid inference issues)
    var isVoiceEnabled: Boolean by remember { mutableStateOf(prefs.isVoiceCommandsEnabled) }
    var isWakeWordEnabled: Boolean by remember { mutableStateOf(prefs.isWakeWordEnabled) }
    var isAudioFeedbackEnabled: Boolean by remember { mutableStateOf(prefs.isAudioFeedbackEnabled) }
    var isVibrateOnCommand: Boolean by remember { mutableStateOf(prefs.isVibrateOnCommand) }
    var isAutoPunctuationEnabled: Boolean by remember { mutableStateOf(prefs.isAutoPunctuationEnabled) }
    var isPrivacyModeEnabled: Boolean by remember { mutableStateOf(prefs.isPrivacyModeEnabled) }
    
    // Available voice command languages
    val languages = listOf(
        "English (US)" to "en-US",
        "English (UK)" to "en-GB",
        "Spanish" to "es-ES",
        "French" to "fr-FR",
        "German" to "de-DE",
        "Italian" to "it-IT",
        "Japanese" to "ja-JP",
        "Korean" to "ko-KR",
        "Chinese (Simplified)" to "zh-CN",
        "Chinese (Traditional)" to "zh-TW"
    )
    
    var selectedLanguage: String by remember { mutableStateOf(prefs.voiceCommandLanguage) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Command Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.medium)
        ) {
            // Voice Commands Section
            item { 
                SectionHeader(
                    title = "Voice Commands",
                    icon = Icons.Default.KeyboardVoice
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Enable Voice Commands",
                    description = "Allow using voice commands in the app",
                    icon = Icons.Default.Mic,
                    isChecked = isVoiceEnabled,
                    onCheckedChange = { 
                        isVoiceEnabled = it
                        prefs.isVoiceCommandsEnabled = it
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Wake Word Detection",
                    description = "Respond to wake word (e.g., 'Hey AI' or 'OK AI')",
                    icon = Icons.Default.VoiceChat,
                    isChecked = isWakeWordEnabled,
                    enabled = isVoiceEnabled,
                    onCheckedChange = { 
                        isWakeWordEnabled = it
                        prefs.isWakeWordEnabled = it
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Audio Feedback",
                    description = "Play sounds for command confirmation",
                    icon = Icons.Default.VolumeUp,
                    isChecked = isAudioFeedbackEnabled,
                    onCheckedChange = { 
                        isAudioFeedbackEnabled = it
                        prefs.isAudioFeedbackEnabled = it
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Vibrate on Command",
                    description = "Provide haptic feedback for voice commands",
                    icon = Icons.Default.Vibration,
                    isChecked = isVibrateOnCommand,
                    onCheckedChange = { 
                        isVibrateOnCommand = it
                        prefs.isVibrateOnCommand = it
                    }
                )
            }
            
            // Language Section
            item { 
                SectionHeader(
                    title = "Language",
                    icon = Icons.Default.Language
                )
            }
            
            item {
                var expanded by remember { mutableStateOf(false) }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    PreferenceItem(
                        title = "Language",
                        summary = "Select preferred language for voice commands",
                        onClick = { expanded = true }
                    )
                    
                    // Language selection dropdown
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        languages.forEach { (name, code) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedLanguage = code
                                    prefs.voiceCommandLanguage = code
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (selectedLanguage == code) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    // Display selected language
                    Text(
                        text = languages.find { it.second == selectedLanguage }?.first ?: "English (US)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    )
                }
            }
            
            // Privacy Section
            item { 
                SectionHeader(
                    title = "Privacy",
                    icon = Icons.Default.PrivacyTip
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Privacy Mode",
                    description = "Prevent sending voice recordings to cloud services",
                    icon = Icons.Default.Security,
                    isChecked = isPrivacyModeEnabled,
                    onCheckedChange = { 
                        isPrivacyModeEnabled = it
                        prefs.isPrivacyModeEnabled = it
                        
                        if (it) {
                            // Disable features that require cloud processing
                            isAutoPunctuationEnabled = false
                            prefs.isAutoPunctuationEnabled = false
                        }
                    }
                )
            }
            
            item {
                PreferenceSwitch(
                    title = "Auto Punctuation",
                    description = "Automatically add punctuation to voice input",
                    icon = Icons.Default.EditNote,
                    isChecked = isAutoPunctuationEnabled,
                    enabled = !isPrivacyModeEnabled,
                    onCheckedChange = { 
                        isAutoPunctuationEnabled = it
                        prefs.isAutoPunctuationEnabled = it
                    }
                )
            }
            
            item {
                TextButton(
                    onClick = { /* Show privacy policy */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("View Voice Data Privacy Policy")
                }
            }
            
            // Voice Command List
            item { 
                SectionHeader(
                    title = "Available Commands",
                    icon = Icons.Default.HelpOutline
                )
            }
            
            item {
                TextButton(
                    onClick = { /* Show command list */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("View All Voice Commands")
                }
            }
            
            // Add some bottom padding
            item { 
                Spacer(modifier = Modifier.height(24.dp)) 
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceCommandSettingsScreenPreview() {
    MaterialTheme {
        VoiceCommandSettingsScreen(
            onBack = {}
        )
    }
}

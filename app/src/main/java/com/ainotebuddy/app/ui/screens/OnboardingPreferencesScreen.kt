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
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.ModernColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPreferencesScreen(
    preferencesManager: PreferencesManager,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // User preferences state
    var selectedTheme by remember { mutableStateOf(PreferencesManager.THEME_SYSTEM) }
    var fontSizeScale by remember { mutableFloatStateOf(1f) }
    var enableAnalytics by remember { mutableStateOf(true) }
    var enableBackup by remember { mutableStateOf(true) }
    
    // Load saved preferences when preferencesManager changes
    LaunchedEffect(preferencesManager) {
        selectedTheme = preferencesManager.getTheme()
        fontSizeScale = preferencesManager.getFontScale()
        enableAnalytics = preferencesManager.getAnalyticsEnabled()
        enableBackup = preferencesManager.getBackupEnabled()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalize Your Experience") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            // Save preferences
                            preferencesManager.setTheme(selectedTheme)
                            preferencesManager.setFontScale(fontSizeScale)
                            preferencesManager.setAnalyticsEnabled(enableAnalytics)
                            preferencesManager.setBackupEnabled(enableBackup)
                            onComplete()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModernColors.AIPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Get Started")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Make it yours",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Customize your experience to match your preferences and workflow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Theme Selection
            Column {
                PreferenceTitle(text = "Appearance")
                PreferenceItem(
                    title = "Theme",
                    summary = "Choose your preferred theme",
                    onClick = { /* Handle theme selection */ }
                )
                ThemeSelectionChips(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { theme -> selectedTheme = theme }
                )
                
                PreferenceSlider(
                    title = "Font Size",
                    summary = "Adjust text size",
                    value = fontSizeScale,
                    onValueChange = { fontSizeScale = it },
                    valueRange = 0.8f..1.5f,
                    steps = 7
                )
            }
            
            // Features
            Column {
                PreferenceTitle(text = "Features")
                PreferenceSwitch(
                    title = "Analytics",
                    description = "Help improve AI NoteBuddy by sharing usage data",
                    icon = Icons.Default.Analytics,
                    isChecked = enableAnalytics,
                    onCheckedChange = { enableAnalytics = it }
                )
                
                PreferenceSwitch(
                    title = "Auto Backup",
                    description = "Automatically back up your notes to the cloud",
                    icon = Icons.Default.CloudUpload,
                    isChecked = enableBackup,
                    onCheckedChange = { enableBackup = it }
                )
            }
            
            // Data Import
            Column {
                PreferenceTitle(text = "Get Started")
                PreferenceItem(
                    title = "Import Notes",
                    summary = "Bring your notes from other apps",
                    onClick = { /* Handle import */ }
                )

                PreferenceItem(
                    title = "Sync Accounts",
                    summary = "Connect your cloud accounts",
                    onClick = { /* Handle account sync */ }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectionChips(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themes = listOf(
        "Light" to Icons.Default.LightMode,
        "Dark" to Icons.Default.DarkMode,
        "System" to Icons.Default.Settings
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { (theme, icon) ->
            val isSelected = selectedTheme.equals(theme, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onThemeSelected(theme) },
                label = { Text(theme) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

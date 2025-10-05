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
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.TutorialPreferences
import com.ainotebuddy.app.ui.components.PreferenceItem
import com.ainotebuddy.app.ui.theme.ModernColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onShowTutorials: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var darkTheme by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(1f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            // Dark Mode Toggle
            PreferenceItem(
                title = "Dark Theme",
                icon = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                onClick = { darkTheme = !darkTheme; onThemeChange(darkTheme) },
                endContent = {
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it; onThemeChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ModernColors.AIPrimary,
                            checkedTrackColor = ModernColors.AIPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            )
            
            // Font Size
            PreferenceItem(
                title = "Font Size",
                icon = Icons.Default.TextFields,
                onClick = {},
                endContent = {
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it; onFontSizeChange(it) },
                        valueRange = 0.8f..1.5f,
                        steps = 6,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            )
            
            // Data Section
            Text(
                text = "Data & Storage",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            // Export Notes
            PreferenceItem(
                title = "Export Notes",
                icon = Icons.Default.FileDownload,
                onClick = { /* Handle export */ }
            )
            
            // Backup & Restore
            PreferenceItem(
                title = "Backup & Restore",
                icon = Icons.Default.CloudUpload,
                onClick = { /* Handle backup */ }
            )
            
            // Tutorials Section
            Text(
                text = "Tutorials",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            // Reset Tutorials
            var showResetDialog by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current
            
            PreferenceItem(
                title = "Reset Tutorials",
                subtitle = "Reset all tutorial progress",
                icon = Icons.Default.Help,
                onClick = { showResetDialog = true }
            )
            
            // Show Tutorials Again
            PreferenceItem(
                title = "Show Tutorials Again",
                subtitle = "Replay all tutorials",
                icon = Icons.Default.Replay,
                onClick = {
                    coroutineScope.launch {
                        TutorialPreferences.getInstance(context).resetTutorialProgress()
                        onShowTutorials()
                    }
                }
            )

            // Reminders section
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            val repo = remember { com.ainotebuddy.app.data.preferences.SettingsRepository(context) }
            val scope = rememberCoroutineScope()
            val defaultHour by repo.reminderDefaultHour.collectAsState(initial = 9)
            val defaultMinute by repo.reminderDefaultMinute.collectAsState(initial = 0)

            var hour by remember(defaultHour) { mutableIntStateOf(defaultHour) }
            var minute by remember(defaultMinute) { mutableIntStateOf(defaultMinute) }

            PreferenceItem(
                title = "Default reminder time",
                subtitle = "%02d:%02d".format(hour, minute),
                icon = Icons.Default.Schedule,
                onClick = {},
                endContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Hour: ", modifier = Modifier.padding(end = 8.dp))
                        Slider(value = hour.toFloat(), onValueChange = { hour = it.toInt() }, valueRange = 0f..23f, steps = 22, modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Minute: ", modifier = Modifier.padding(end = 8.dp))
                        Slider(value = minute.toFloat(), onValueChange = { minute = it.toInt() }, valueRange = 0f..59f, steps = 58, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { hour = defaultHour; minute = defaultMinute }) { Text("Reset") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { scope.launch { repo.setReminderDefaultTime(hour, minute) } }) { Text("Save") }
                    }
                }
            )

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            // Version
            PreferenceItem(
                title = "Version",
                subtitle = "1.0.0",
                icon = Icons.Default.Info,
                onClick = {}
            )
            
            // Reset Tutorial Dialog
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset Tutorials") },
                    text = { Text("Are you sure you want to reset all tutorial progress? This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    TutorialPreferences.getInstance(context).resetTutorialProgress()
                                    showResetDialog = false
                                }
                            }
                        ) {
                            Text("Reset")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showResetDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

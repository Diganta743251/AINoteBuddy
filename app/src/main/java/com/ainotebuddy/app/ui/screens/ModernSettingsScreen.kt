package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.TutorialPreferences
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.semanticColors
import com.ainotebuddy.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSettingsScreen(
    repository: NoteRepository,
    onSignOut: () -> Unit,
    onSyncToDrive: () -> Unit,
    onSyncFromDrive: () -> Unit,
    onAISettingsClick: () -> Unit,
    onThemeTestClick: () -> Unit,
    onSecuritySettingsClick: () -> Unit,
    accessibilityPrefs: AccessibilityPreferences,
    onAccessibilityChange: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSyncDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showTutorialResetDialog by remember { mutableStateOf(false) }
    var showTutorialResetSuccess by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Modern top app bar
        ModernTopAppBar(
            title = "Settings",
            subtitle = "Customize your experience"
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Account Section
            item {
                SettingsSection(title = "Account") {
                    SettingsCard(
                        title = "Sync & Backup",
                        description = "Keep your notes safe in the cloud",
                        icon = Icons.Default.CloudSync,
                        onClick = { showSyncDialog = true }
                    )
                    
                    SettingsCard(
                        title = "Sign Out",
                        description = "Sign out of your account",
                        icon = Icons.Default.Logout,
                        onClick = { showSignOutDialog = true },
                        isDestructive = true
                    )
                }
            }
            
            // Security & Privacy Section
            item {
                SettingsSection(title = "Security & Privacy") {
                    SettingsCard(
                        title = "Security Settings",
                        description = "Manage note security and privacy",
                        icon = Icons.Default.Security,
                        onClick = onSecuritySettingsClick
                    )
                    
                    SettingsCard(
                        title = "App Lock",
                        description = "Secure the app with a PIN or biometrics",
                        icon = Icons.Default.Lock,
                        onClick = { /* Handle app lock settings */ }
                    )
                    
                    SettingsCard(
                        title = "Privacy Controls",
                        description = "Manage your data and privacy settings",
                        icon = Icons.Default.PrivacyTip,
                        onClick = { /* Handle privacy controls */ }
                    )
                }
            }
            
            // AI & Intelligence Section
            item {
                SettingsSection(title = "AI & Intelligence") {
                    SettingsCard(
                        title = "AI Settings",
                        description = "Configure AI features and preferences",
                        icon = Icons.Default.Psychology,
                        onClick = onAISettingsClick
                    )
                    
                    SettingsCard(
                        title = "Smart Suggestions",
                        description = "Get AI-powered note suggestions",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { /* Handle smart suggestions settings */ }
                    )
                    
                    SettingsCard(
                        title = "Voice Commands",
                        description = "Control the app with your voice",
                        icon = Icons.Default.Mic,
                        onClick = { /* Handle voice settings */ }
                    )
                }
            }
            
            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    // Theme selection card
                    SettingsCard(
                        title = "Themes & Colors",
                        description = "Choose from beautiful themes and customize colors",
                        icon = Icons.Default.Palette,
                        onClick = onThemeTestClick
                    )
                    
                    // Theme test card (kept for backward compatibility)
                    SettingsCard(
                        title = "Theme Test Screen",
                        description = "Test theme changes in real-time",
                        icon = Icons.Default.ColorLens,
                        onClick = onThemeTestClick
                    )
                    
                    SettingsCard(
                        title = "Typography",
                        description = "Adjust text size and fonts",
                        icon = Icons.Default.TextFields,
                        onClick = { /* Handle typography settings */ }
                    )
                    
                    SettingsCard(
                        title = "Layout",
                        description = "Customize the app layout",
                        icon = Icons.Default.ViewQuilt,
                        onClick = { /* Handle layout settings */ }
                    )
                }
            }
            
            // Accessibility Section
            item {
                SettingsSection(title = "Accessibility") {
                    AccessibilitySettingsCard(
                        preferences = accessibilityPrefs,
                        onPreferencesChange = onAccessibilityChange
                    )
                }
            }
            
            // Privacy & Security Section
            item {
                SettingsSection(title = "Privacy & Security") {
                    SettingsCard(
                        title = "Privacy Settings",
                        description = "Control your data and privacy",
                        icon = Icons.Default.Security,
                        onClick = { /* Handle privacy settings */ }
                    )
                    
                    SettingsCard(
                        title = "Data Export",
                        description = "Export your notes and data",
                        icon = Icons.Default.Download,
                        onClick = { /* Handle data export */ }
                    )
                    
                    SettingsCard(
                        title = "Security Vault",
                        description = "Secure your sensitive notes",
                        icon = Icons.Default.Lock,
                        onClick = { /* Handle security vault */ }
                    )
                }
            }
            
            // Advanced Section
            item {
                SettingsSection(title = "Advanced") {
                    SettingsCard(
                        title = "Developer Options",
                        description = "Advanced settings for power users",
                        icon = Icons.Default.DeveloperMode,
                        onClick = { /* Handle developer options */ }
                    )
                    
                    SettingsCard(
                        title = "Debug Information",
                        description = "View app diagnostics",
                        icon = Icons.Default.BugReport,
                        onClick = { /* Handle debug info */ }
                    )
                }
            }
            
            // Tutorials Section
            item {
                SettingsSection(title = "Tutorials") {
                    SettingsCard(
                        title = "Reset Tutorials",
                        description = "Clear all tutorial progress",
                        icon = Icons.Default.Refresh,
                        onClick = { showTutorialResetDialog = true },
                        isDestructive = true
                    )
                    
                    SettingsCard(
                        title = "Show Tutorials Again",
                        description = "Replay all tutorials from the beginning",
                        icon = Icons.Default.Replay,
                        onClick = {
                            coroutineScope.launch {
                                TutorialPreferences.getInstance(context).resetTutorialProgress()
                                showTutorialResetSuccess = true
                            }
                        }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    SettingsCard(
                        title = "App Version",
                        description = "AI NoteBuddy v2.0.0",
                        icon = Icons.Default.Info,
                        onClick = { /* Handle version info */ }
                    )
                    
                    SettingsCard(
                        title = "Help & Support",
                        description = "Get help and contact support",
                        icon = Icons.Default.Help,
                        onClick = { /* Handle help */ }
                    )
                    
                    SettingsCard(
                        title = "Privacy Policy",
                        description = "Read our privacy policy",
                        icon = Icons.Default.Policy,
                        onClick = { /* Handle privacy policy */ }
                    )
                }
            }
        }
    }
    
    // Sync Dialog
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync & Backup") },
            text = { 
                Text("Choose how you want to sync your notes:")
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            onSyncToDrive()
                            showSyncDialog = false
                        }
                    ) {
                        Text("Backup to Cloud")
                    }
                    TextButton(
                        onClick = {
                            onSyncFromDrive()
                            showSyncDialog = false
                        }
                    ) {
                        Text("Restore from Cloud")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { 
                Text("Are you sure you want to sign out? Make sure your notes are backed up.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSignOut()
                        showSignOutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.semanticColors().error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Tutorial Reset Confirmation Dialog
    if (showTutorialResetDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialResetDialog = false },
            title = { Text("Reset Tutorials") },
            text = { 
                Text("This will reset all tutorial progress. You'll need to go through the tutorials again. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            TutorialPreferences.getInstance(context).resetTutorialProgress()
                            showTutorialResetDialog = false
                            showTutorialResetSuccess = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.semanticColors().error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTutorialResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Tutorial Reset Success Snackbar
    if (showTutorialResetSuccess) {
        LaunchedEffect(showTutorialResetSuccess) {
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            showTutorialResetSuccess = false
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                IconButton(onClick = { showTutorialResetSuccess = false }) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
        ) {
            Text("Tutorial progress has been reset")
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        content()
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    QuickActionCard(
        title = title,
        description = description,
        icon = icon,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        accentColor = if (isDestructive) {
            MaterialTheme.semanticColors().error
        } else {
            MaterialTheme.colorScheme.primary
        }
    )
}

@Composable
fun AccessibilitySettingsCard(
    preferences: AccessibilityPreferences,
    onPreferencesChange: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Accessibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = "Accessibility Options",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize for better accessibility",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Accessibility toggles
            HighContrastToggle(
                enabled = preferences.highContrastMode,
                onToggle = { onPreferencesChange(preferences.copy(highContrastMode = it)) }
            )
            
            ReducedMotionToggle(
                enabled = preferences.reducedMotion,
                onToggle = { onPreferencesChange(preferences.copy(reducedMotion = it)) }
            )
            
            LargeTextToggle(
                enabled = preferences.largeText,
                onToggle = { onPreferencesChange(preferences.copy(largeText = it)) }
            )
        }
    }
}

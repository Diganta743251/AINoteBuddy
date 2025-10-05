package com.ainotebuddy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.semanticColors

data class SettingsGroup(
    val title: String,
    val icon: ImageVector,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
    val hasSwitch: Boolean = false,
    val switchState: Boolean = false,
    val onSwitchChange: ((Boolean) -> Unit)? = null,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveSettingsScreen(
    repository: NoteRepository,
    onSignOut: () -> Unit,
    onSyncToDrive: () -> Unit,
    onSyncFromDrive: () -> Unit,
    onAISettingsClick: () -> Unit,
    accessibilityPrefs: AccessibilityPreferences,
    onAccessibilityChange: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Settings state
    var darkMode by remember { mutableStateOf(false) }
    var autoSync by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricLock by remember { mutableStateOf(false) }
    var autoSave by remember { mutableStateOf(true) }
    var voiceCommands by remember { mutableStateOf(false) }
    var collaborationMode by remember { mutableStateOf(true) }
    var analyticsEnabled by remember { mutableStateOf(false) }
    
    // Dialog states
    var showSyncDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    
    val settingsGroups = remember(
        darkMode, autoSync, notificationsEnabled, biometricLock, 
        autoSave, voiceCommands, collaborationMode, analyticsEnabled
    ) {
        createSettingsGroups(
            darkMode = darkMode,
            onDarkModeChange = { darkMode = it },
            autoSync = autoSync,
            onAutoSyncChange = { autoSync = it },
            notificationsEnabled = notificationsEnabled,
            onNotificationsChange = { notificationsEnabled = it },
            biometricLock = biometricLock,
            onBiometricLockChange = { biometricLock = it },
            autoSave = autoSave,
            onAutoSaveChange = { autoSave = it },
            voiceCommands = voiceCommands,
            onVoiceCommandsChange = { voiceCommands = it },
            collaborationMode = collaborationMode,
            onCollaborationModeChange = { collaborationMode = it },
            analyticsEnabled = analyticsEnabled,
            onAnalyticsChange = { analyticsEnabled = it },
            onSyncClick = { showSyncDialog = true },
            onSignOutClick = { showSignOutDialog = true },
            onExportClick = { showExportDialog = true },
            onDeleteAllClick = { showDeleteAllDialog = true },
            onAISettingsClick = onAISettingsClick
        )
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Modern top app bar
        ModernTopAppBar(
            title = "Settings",
            subtitle = "Customize your AI NoteBuddy experience",
            actions = {
                IconButton(onClick = { /* Search settings */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search settings")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User profile section
            item {
                UserProfileSection(
                    userName = "John Doe", // Get from user preferences
                    userEmail = "john.doe@example.com", // Get from user preferences
                    profilePicture = null, // Get from user preferences
                    onEditProfile = { /* Handle profile editing */ }
                )
            }
            
            // Accessibility section (always visible)
            item {
                AccessibilitySection(
                    preferences = accessibilityPrefs,
                    onPreferencesChange = onAccessibilityChange
                )
            }
            
            // Settings groups
            items(settingsGroups) { group ->
                SettingsGroupSection(
                    group = group,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // App info section
            item {
                AppInfoSection(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // Dialogs
    if (showSyncDialog) {
        SyncDialog(
            onDismiss = { showSyncDialog = false },
            onSyncToDrive = {
                onSyncToDrive()
                showSyncDialog = false
            },
            onSyncFromDrive = {
                onSyncFromDrive()
                showSyncDialog = false
            }
        )
    }
    
    if (showSignOutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out? Make sure your notes are backed up.",
            confirmText = "Sign Out",
            onConfirm = {
                onSignOut()
                showSignOutDialog = false
            },
            onDismiss = { showSignOutDialog = false },
            isDestructive = true
        )
    }
    
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                // Handle export
                showExportDialog = false
            }
        )
    }
    
    if (showDeleteAllDialog) {
        ConfirmationDialog(
            title = "Delete All Notes",
            message = "This will permanently delete all your notes. This action cannot be undone.",
            confirmText = "Delete All",
            onConfirm = {
                // Handle delete all
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false },
            isDestructive = true
        )
    }
}

@Composable
private fun UserProfileSection(
    userName: String,
    userEmail: String,
    profilePicture: String?,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile picture placeholder
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicture != null) {
                        // Load profile picture
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Premium Member",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(onClick = onEditProfile) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AccessibilitySection(
    preferences: AccessibilityPreferences,
    onPreferencesChange: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Accessibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            AccessibilitySettingsCard(
                preferences = preferences,
                onPreferencesChange = onPreferencesChange
            )
        }
    }
}

@Composable
private fun SettingsGroupSection(
    group: SettingsGroup,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    imageVector = group.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            group.items.forEachIndexed { index, item ->
                SettingsItemRow(
                    item = item,
                    showDivider = index < group.items.size - 1
                )
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.isDestructive) {
                    MaterialTheme.semanticColors().error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (item.isDestructive) {
                            MaterialTheme.semanticColors().error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    item.badge?.let { badge ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (item.hasSwitch && item.onSwitchChange != null) {
                Switch(
                    checked = item.switchState,
                    onCheckedChange = item.onSwitchChange
                )
            } else {
                IconButton(onClick = item.onClick) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 36.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun AppInfoSection(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "App Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "AI NoteBuddy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Version 2.0.0 (Modern UI Edition)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your intelligent note-taking companion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Dialog composables
@Composable
private fun SyncDialog(
    onDismiss: () -> Unit,
    onSyncToDrive: () -> Unit,
    onSyncFromDrive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync & Backup") },
        text = {
            Column {
                Text("Choose how you want to sync your notes:")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSyncToDrive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup")
                    }
                    
                    OutlinedButton(
                        onClick = onSyncFromDrive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.semanticColors().error
                    )
                } else {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
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
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val exportFormats = listOf("PDF", "TXT", "JSON", "Markdown")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Notes") },
        text = {
            Column {
                Text("Choose export format:")
                Spacer(modifier = Modifier.height(16.dp))
                
                exportFormats.forEach { format ->
                    OutlinedButton(
                        onClick = { onExport(format) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(format)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to create settings groups
private fun createSettingsGroups(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    biometricLock: Boolean,
    onBiometricLockChange: (Boolean) -> Unit,
    autoSave: Boolean,
    onAutoSaveChange: (Boolean) -> Unit,
    voiceCommands: Boolean,
    onVoiceCommandsChange: (Boolean) -> Unit,
    collaborationMode: Boolean,
    onCollaborationModeChange: (Boolean) -> Unit,
    analyticsEnabled: Boolean,
    onAnalyticsChange: (Boolean) -> Unit,
    onSyncClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onAISettingsClick: () -> Unit
): List<SettingsGroup> {
    return listOf(
        SettingsGroup(
            title = "Appearance",
            icon = Icons.Default.Palette,
            items = listOf(
                SettingsItem(
                    title = "Dark Mode",
                    description = "Use dark theme throughout the app",
                    icon = Icons.Default.DarkMode,
                    onClick = { onDarkModeChange(!darkMode) },
                    hasSwitch = true,
                    switchState = darkMode,
                    onSwitchChange = onDarkModeChange
                ),
                SettingsItem(
                    title = "Theme Customization",
                    description = "Customize colors and appearance",
                    icon = Icons.Default.ColorLens,
                    onClick = { /* Handle theme customization */ }
                ),
                SettingsItem(
                    title = "Font Settings",
                    description = "Adjust text size and font family",
                    icon = Icons.Default.TextFields,
                    onClick = { /* Handle font settings */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "AI & Intelligence",
            icon = Icons.Default.Psychology,
            items = listOf(
                SettingsItem(
                    title = "AI Settings",
                    description = "Configure AI features and preferences",
                    icon = Icons.Default.Settings,
                    onClick = onAISettingsClick,
                    badge = "New"
                ),
                SettingsItem(
                    title = "Voice Commands",
                    description = "Control the app with voice",
                    icon = Icons.Default.Mic,
                    onClick = { onVoiceCommandsChange(!voiceCommands) },
                    hasSwitch = true,
                    switchState = voiceCommands,
                    onSwitchChange = onVoiceCommandsChange
                ),
                SettingsItem(
                    title = "Smart Suggestions",
                    description = "Get AI-powered note suggestions",
                    icon = Icons.Default.AutoAwesome,
                    onClick = { /* Handle smart suggestions */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "Notes & Editor",
            icon = Icons.Default.Edit,
            items = listOf(
                SettingsItem(
                    title = "Auto-Save",
                    description = "Automatically save notes while typing",
                    icon = Icons.Default.Save,
                    onClick = { onAutoSaveChange(!autoSave) },
                    hasSwitch = true,
                    switchState = autoSave,
                    onSwitchChange = onAutoSaveChange
                ),
                SettingsItem(
                    title = "Default Note Format",
                    description = "Choose default formatting for new notes",
                    icon = Icons.Default.FormatPaint,
                    onClick = { /* Handle default format */ }
                ),
                SettingsItem(
                    title = "Templates",
                    description = "Manage note templates",
                    icon = Icons.Default.Description,
                    onClick = { /* Handle templates */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "Sync & Backup",
            icon = Icons.Default.CloudSync,
            items = listOf(
                SettingsItem(
                    title = "Auto Sync",
                    description = "Automatically sync notes to cloud",
                    icon = Icons.Default.Sync,
                    onClick = { onAutoSyncChange(!autoSync) },
                    hasSwitch = true,
                    switchState = autoSync,
                    onSwitchChange = onAutoSyncChange
                ),
                SettingsItem(
                    title = "Sync & Backup",
                    description = "Manage cloud sync and backups",
                    icon = Icons.Default.Cloud,
                    onClick = onSyncClick
                ),
                SettingsItem(
                    title = "Export Notes",
                    description = "Export your notes to various formats",
                    icon = Icons.Default.Download,
                    onClick = onExportClick
                )
            )
        ),
        
        SettingsGroup(
            title = "Privacy & Security",
            icon = Icons.Default.Security,
            items = listOf(
                SettingsItem(
                    title = "Biometric Lock",
                    description = "Use fingerprint or face unlock",
                    icon = Icons.Default.Fingerprint,
                    onClick = { onBiometricLockChange(!biometricLock) },
                    hasSwitch = true,
                    switchState = biometricLock,
                    onSwitchChange = onBiometricLockChange
                ),
                SettingsItem(
                    title = "Privacy Settings",
                    description = "Control data sharing and privacy",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { /* Handle privacy settings */ }
                ),
                SettingsItem(
                    title = "Security Vault",
                    description = "Secure your sensitive notes",
                    icon = Icons.Default.Lock,
                    onClick = { /* Handle security vault */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "Collaboration",
            icon = Icons.Default.Group,
            items = listOf(
                SettingsItem(
                    title = "Collaboration Mode",
                    description = "Enable real-time collaboration",
                    icon = Icons.Default.People,
                    onClick = { onCollaborationModeChange(!collaborationMode) },
                    hasSwitch = true,
                    switchState = collaborationMode,
                    onSwitchChange = onCollaborationModeChange
                ),
                SettingsItem(
                    title = "Sharing Settings",
                    description = "Configure note sharing options",
                    icon = Icons.Default.Share,
                    onClick = { /* Handle sharing settings */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "Notifications",
            icon = Icons.Default.Notifications,
            items = listOf(
                SettingsItem(
                    title = "Push Notifications",
                    description = "Receive notifications for updates",
                    icon = Icons.Default.NotificationsActive,
                    onClick = { onNotificationsChange(!notificationsEnabled) },
                    hasSwitch = true,
                    switchState = notificationsEnabled,
                    onSwitchChange = onNotificationsChange
                ),
                SettingsItem(
                    title = "Reminder Settings",
                    description = "Configure note reminders",
                    icon = Icons.Default.Schedule,
                    onClick = { /* Handle reminder settings */ }
                )
            )
        ),
        
        SettingsGroup(
            title = "Advanced",
            icon = Icons.Default.Settings,
            items = listOf(
                SettingsItem(
                    title = "Analytics",
                    description = "Help improve the app with usage data",
                    icon = Icons.Default.Analytics,
                    onClick = { onAnalyticsChange(!analyticsEnabled) },
                    hasSwitch = true,
                    switchState = analyticsEnabled,
                    onSwitchChange = onAnalyticsChange
                ),
                SettingsItem(
                    title = "Developer Options",
                    description = "Advanced settings for power users",
                    icon = Icons.Default.DeveloperMode,
                    onClick = { /* Handle developer options */ }
                ),
                SettingsItem(
                    title = "Clear Cache",
                    description = "Clear app cache and temporary files",
                    icon = Icons.Default.CleaningServices,
                    onClick = { /* Handle clear cache */ }
                ),
                SettingsItem(
                    title = "Delete All Notes",
                    description = "Permanently delete all notes",
                    icon = Icons.Default.DeleteForever,
                    onClick = onDeleteAllClick,
                    isDestructive = true
                )
            )
        ),
        
        SettingsGroup(
            title = "Account",
            icon = Icons.Default.AccountCircle,
            items = listOf(
                SettingsItem(
                    title = "Account Settings",
                    description = "Manage your account information",
                    icon = Icons.Default.Person,
                    onClick = { /* Handle account settings */ }
                ),
                SettingsItem(
                    title = "Subscription",
                    description = "Manage your premium subscription",
                    icon = Icons.Default.Star,
                    onClick = { /* Handle subscription */ },
                    badge = "Premium"
                ),
                SettingsItem(
                    title = "Sign Out",
                    description = "Sign out of your account",
                    icon = Icons.Default.Logout,
                    onClick = onSignOutClick,
                    isDestructive = true
                )
            )
        ),
        
        SettingsGroup(
            title = "Help & Support",
            icon = Icons.Default.Help,
            items = listOf(
                SettingsItem(
                    title = "Help Center",
                    description = "Get help and find answers",
                    icon = Icons.Default.HelpOutline,
                    onClick = { /* Handle help center */ }
                ),
                SettingsItem(
                    title = "Contact Support",
                    description = "Get in touch with our support team",
                    icon = Icons.Default.ContactSupport,
                    onClick = { /* Handle contact support */ }
                ),
                SettingsItem(
                    title = "Send Feedback",
                    description = "Share your thoughts and suggestions",
                    icon = Icons.Default.Feedback,
                    onClick = { /* Handle feedback */ }
                ),
                SettingsItem(
                    title = "Privacy Policy",
                    description = "Read our privacy policy",
                    icon = Icons.Default.Policy,
                    onClick = { /* Handle privacy policy */ }
                ),
                SettingsItem(
                    title = "Terms of Service",
                    description = "Read our terms of service",
                    icon = Icons.Default.Gavel,
                    onClick = { /* Handle terms of service */ }
                )
            )
        )
    )
}
